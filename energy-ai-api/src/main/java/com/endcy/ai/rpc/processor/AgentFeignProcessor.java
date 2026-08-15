package com.endcy.ai.rpc.processor;

import cn.hutool.core.util.StrUtil;
import com.endcy.ai.agent.executor.*;
import com.endcy.ai.manager.LbCredentialManager;
import com.endcy.ai.repository.service.AgentSessionRepository;
import com.endcy.ai.rpc.api.AgentFeignService;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.request.AgentTaskParam;
import com.endcy.ai.rpc.domain.response.AgentTaskRet;
import com.endcy.ai.rpc.domain.response.ToolInventoryRet;
import com.endcy.ai.tools.registry.ToolMeta;
import com.endcy.ai.tools.registry.ToolRegistry;
import com.endcy.service.common.annotation.LogReqRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Agent Feign 接口实现 —— 向外部服务暴露 agent 能力。
 *
 * <p>路径：{@code /api/agent/*}（仅内部调用，不暴露到公网）。</p>
 *
 * @author endcy
 * @since 2026/08/08
 */
@LogReqRes("log.enable.rpc.AgentFeignProcessor")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/agent")
public class AgentFeignProcessor implements AgentFeignService {

    private final AgentTaskService agentTaskService;
    private final AgentExecutor agentExecutor;
    private final AgentSessionRepository agentSessionRepository;
    private final ToolRegistry toolRegistry;
    private final LbCredentialManager lbCredentialManager;
    private final AgentEventPublisher agentEventPublisher;
    private final StringRedisTemplate stringRedisTemplate;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @PostMapping("/task/submit")
    public CommonResMsgDTO<AgentTaskRet> submitTask(@RequestBody AgentTaskParam param) {
        log.info("Feign 提交任务: mode={}, chatId={}, question={}",
                param.getMode(), param.getChatId(), StrUtil.maxLength(param.getQuestion(), 80));

        AgentMode mode = parseMode(param.getMode());
        String taskId = agentTaskService.submitTask(
                mode, param.getChatId(), param.getGroupId(), param.getQuestion(),
                param.getScopeType(), param.getUserRole());

        AgentTaskRet ret = buildTaskRet(taskId, mode, param);
        ret.setStatus("SUBMITTED");
        return CommonResMsgDTO.successDeviceRes(ret);
    }

    @Override
    @GetMapping("/task/{taskId}")
    public CommonResMsgDTO<AgentTaskRet> getTaskStatus(@PathVariable String taskId) {
        AgentSession session = agentTaskService.getTask(taskId);
        if (session == null) {
            return CommonResMsgDTO.errorDeviceRes("任务不存在: " + taskId);
        }
        return CommonResMsgDTO.successDeviceRes(convertToRet(session));
    }

    @Override
    @PostMapping("/task/{taskId}/cancel")
    public CommonResMsgDTO<Boolean> cancelTask(@PathVariable String taskId,
                                               @RequestParam(required = false) String reason) {
        boolean ok = agentTaskService.cancel(taskId, reason);
        return CommonResMsgDTO.successDeviceRes(ok);
    }

    @Override
    @GetMapping("/task/list")
    public CommonResMsgDTO<List<AgentTaskRet>> listTasksByChatId(@RequestParam Long chatId) {
        List<com.endcy.ai.repository.domain.entity.AgentSession> sessions =
                agentSessionRepository.listByChatId(chatId);
        List<AgentTaskRet> result = new ArrayList<>();
        for (com.endcy.ai.repository.domain.entity.AgentSession s : sessions) {
            result.add(convertEntityToRet(s));
        }
        return CommonResMsgDTO.successDeviceRes(result);
    }

    @Override
    @GetMapping("/tools")
    public CommonResMsgDTO<ToolInventoryRet> listTools() {
        ToolInventoryRet ret = new ToolInventoryRet();
        List<ToolInventoryRet.ToolInfo> tools = new ArrayList<>();

        for (ToolRegistry.RegisteredTool entry : collectAllTools()) {
            ToolMeta meta = entry.getMeta();
            if (meta == null) {
                continue;
            }
            ToolInventoryRet.ToolInfo info = new ToolInventoryRet.ToolInfo();
            info.setName(meta.getName());
            info.setDescription(meta.getLlmDescription());
            info.setBusinessType(meta.getBusinessType());
            info.setRiskLevel(meta.getRiskLevel() != null ? meta.getRiskLevel().name() : "LOW");
            info.setRequiresApproval(meta.isRequiresApproval());
            tools.add(info);
        }

        ret.setTotal(tools.size());
        ret.setTools(tools);
        return CommonResMsgDTO.successDeviceRes(ret);
    }

    @Override
    @PostMapping("/task/execute-sync")
    public CommonResMsgDTO<AgentTaskRet> executeSync(@RequestBody AgentTaskParam param) {
        log.info("Feign 同步执行: mode={}, chatId={}", param.getMode(), param.getChatId());

        AgentMode mode = parseMode(param.getMode());
        AgentSession session = new AgentSession();
        session.setChatId(param.getChatId());
        session.setGroupId(param.getGroupId());
        session.setUserId(param.getUserId());
        session.setMode(mode);
        session.setMaxSteps(param.getMaxSteps());

        agentExecutor.execute(session, param.getQuestion());

        return CommonResMsgDTO.successDeviceRes(convertToRet(session));
    }

    @GetMapping("/credentials/status")
    public CommonResMsgDTO<Map<String, Object>> getCredentialStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", lbCredentialManager.hasCredentials());
        status.put("inCooldown", lbCredentialManager.isInCooldown());
        status.put("cooldownRemainingSeconds", lbCredentialManager.getCooldownRemainingSeconds());
        return CommonResMsgDTO.successDeviceRes(status);
    }

    @PostMapping("/credentials/clear-cooldown")
    public CommonResMsgDTO<Map<String, Object>> clearCredentialCooldown() {
        lbCredentialManager.clearCooldown();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "凭证冷却已清除");
        return CommonResMsgDTO.successDeviceRes(result);
    }

    @GetMapping("/health")
    public CommonResMsgDTO<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("timestamp", System.currentTimeMillis());

        health.put("toolsRegistered", toolRegistry.size());
        health.put("credentialEnabled", lbCredentialManager.hasCredentials());

        try {
            stringRedisTemplate.opsForValue().get("health-check");
            health.put("redis", "UP");
        } catch (Exception e) {
            health.put("redis", "DOWN: " + e.getMessage());
        }

        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            health.put("mysql", "UP");
        } catch (Exception e) {
            health.put("mysql", "DOWN: " + e.getMessage());
        }

        boolean allUp = health.values().stream()
                              .filter(v -> v instanceof String)
                              .allMatch(v -> ((String) v).startsWith("UP"));
        health.put("status", allUp ? "UP" : "DEGRADED");

        return CommonResMsgDTO.successDeviceRes(health);
    }

    /**
     * SSE 流式输出任务执行事件。
     */
    @GetMapping(value = "/task/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AgentEventPublisher.AgentEvent> streamTaskEvents(@PathVariable String taskId) {
        log.info("SSE 订阅任务事件流: taskId={}", taskId);
        return agentEventPublisher.subscribe(taskId)
                                  .doOnComplete(() -> log.debug("SSE 流完成: taskId={}", taskId))
                                  .doOnCancel(() -> log.debug("SSE 流取消: taskId={}", taskId));
    }

    /**
     * 查询任务的思考过程。
     */
    @GetMapping("/task/{taskId}/thoughts")
    public CommonResMsgDTO<List<Map<String, Object>>> getTaskThoughts(@PathVariable String taskId) {
        log.info("查询任务思考过程: taskId={}", taskId);

        AgentSession session = agentTaskService.getTask(taskId);
        if (session != null && !session.getThoughts().isEmpty()) {
            List<Map<String, Object>> thoughts = session.getThoughts().stream()
                                                        .map(this::thoughtToMap)
                                                        .collect(Collectors.toList());
            return CommonResMsgDTO.successDeviceRes(thoughts);
        }

        var dbThoughts = agentSessionRepository.listThoughts(taskId);
        List<Map<String, Object>> thoughts = dbThoughts.stream()
                                                       .map(t -> {
                                                           Map<String, Object> map = new HashMap<>();
                                                           map.put("stepIndex", t.getStepIndex());
                                                           map.put("thought", t.getThought());
                                                           map.put("toolCalls", t.getToolCalls());
                                                           map.put("toolResults", t.getToolResults());
                                                           map.put("durationMs", t.getDurationMs());
                                                           map.put("promptTokens", t.getPromptTokens());
                                                           map.put("completionTokens", t.getCompletionTokens());
                                                           return map;
                                                       })
                                                       .collect(Collectors.toList());
        return CommonResMsgDTO.successDeviceRes(thoughts);
    }

    private Map<String, Object> thoughtToMap(AgentSession.AgentThought thought) {
        Map<String, Object> map = new HashMap<>();
        map.put("stepIndex", thought.getStepIndex());
        map.put("thought", thought.getThought());
        map.put("toolCalls", thought.getToolCalls());
        map.put("toolResults", thought.getToolResults());
        map.put("durationMs", thought.getDurationMs());
        map.put("promptTokens", thought.getPromptTokens());
        map.put("completionTokens", thought.getCompletionTokens());
        return map;
    }

    private AgentMode parseMode(String modeStr) {
        if (StrUtil.isBlank(modeStr)) {
            return AgentMode.AGENTIC;
        }
        try {
            return AgentMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AgentMode.AGENTIC;
        }
    }

    private AgentTaskRet buildTaskRet(String taskId, AgentMode mode, AgentTaskParam param) {
        AgentTaskRet ret = new AgentTaskRet();
        ret.setTaskId(taskId);
        ret.setMode(mode.name());
        ret.setCurrentStep(0);
        ret.setMaxSteps(param.getMaxSteps() != null ? param.getMaxSteps() : 10);
        ret.setTerminal(false);
        return ret;
    }

    private AgentTaskRet convertToRet(AgentSession session) {
        AgentTaskRet ret = new AgentTaskRet();
        ret.setTaskId(session.getSessionId());
        ret.setMode(session.getMode().name());
        ret.setStatus(session.getStatus().name());
        ret.setCurrentStep(session.getCurrentStep());
        ret.setMaxSteps(session.getMaxSteps());
        ret.setFinalAnswer(session.getFinalAnswer());
        ret.setErrorMessage(session.getErrorMessage());
        ret.setTotalPromptTokens(session.getTotalPromptTokens());
        ret.setTotalCompletionTokens(session.getTotalCompletionTokens());
        ret.setTerminal(AgentStateMachine.isTerminal(session.getStatus()));
        if (session.getStartedAt() != null) {
            ret.setStartedAt(java.util.Date.from(session.getStartedAt()));
        }
        if (session.getCompletedAt() != null) {
            ret.setCompletedAt(java.util.Date.from(session.getCompletedAt()));
        }
        return ret;
    }

    private AgentTaskRet convertEntityToRet(com.endcy.ai.repository.domain.entity.AgentSession entity) {
        AgentTaskRet ret = new AgentTaskRet();
        ret.setTaskId(entity.getSessionId());
        ret.setMode(entity.getMode());
        ret.setStatus(entity.getStatus());
        ret.setCurrentStep(entity.getCurrentStep());
        ret.setMaxSteps(10);
        ret.setFinalAnswer(entity.getFinalAnswer());
        ret.setErrorMessage(entity.getErrorMessage());
        ret.setTotalPromptTokens(entity.getTotalPromptTokens());
        ret.setTotalCompletionTokens(entity.getTotalCompletionTokens());
        ret.setTerminal(AgentStateMachine.isTerminal(
                AgentSessionStatus.valueOf(entity.getStatus())));
        ret.setStartedAt(entity.getStartedAt());
        ret.setCompletedAt(entity.getCompletedAt());
        return ret;
    }

    private List<ToolRegistry.RegisteredTool> collectAllTools() {
        List<ToolRegistry.RegisteredTool> result = new ArrayList<>();
        for (String name : toolRegistry.listAll().stream()
                                       .map(tc -> tc.getToolDefinition().name())
                                       .distinct()
                                       .toList()) {
            ToolMeta meta = toolRegistry.getMeta(name);
            if (meta != null) {
                result.add(new ToolRegistry.RegisteredTool(toolRegistry.getByName(name), meta));
            }
        }
        return result;
    }
}
