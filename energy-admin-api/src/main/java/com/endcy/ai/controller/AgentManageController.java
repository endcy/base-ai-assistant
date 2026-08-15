package com.endcy.ai.controller;

import com.endcy.ai.rpc.client.AgentClient;
import com.endcy.ai.rpc.domain.base.CommonResMsgDTO;
import com.endcy.ai.rpc.domain.request.AgentTaskParam;
import com.endcy.ai.rpc.domain.response.AgentTaskRet;
import com.endcy.ai.rpc.domain.response.ToolInventoryRet;
import com.endcy.service.common.annotation.LogRecord;
import com.endcy.service.common.enums.LogActionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Agent 管理接口 —— 智能体任务管理、工具管理、凭证管理。
 * 所有接口通过 AgentClient 调用 energy-ai-api 的内部接口。
 *
 * @author endcy
 * @since 2026-08-10
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agent")
public class AgentManageController {

    private final AgentClient agentClient;

    @PostMapping("/task/submit")
    @LogRecord(value = "提交智能体任务", type = LogActionType.ADD)
    public CommonResMsgDTO<AgentTaskRet> submitTask(@RequestBody AgentTaskParam param) {
        log.info("管理端提交智能体任务: mode={}, chatId={}", param.getMode(), param.getChatId());
        return agentClient.submitTask(param);
    }

    @PostMapping("/task/execute-sync")
    @LogRecord(value = "同步执行智能体任务", type = LogActionType.ADD)
    public CommonResMsgDTO<AgentTaskRet> executeSync(@RequestBody AgentTaskParam param) {
        log.info("管理端同步执行智能体任务: mode={}, chatId={}", param.getMode(), param.getChatId());
        return agentClient.executeSync(param);
    }

    @GetMapping("/task/{taskId}")
    @LogRecord("查询智能体任务状态")
    public CommonResMsgDTO<AgentTaskRet> getTaskStatus(@PathVariable String taskId) {
        return agentClient.getTaskStatus(taskId);
    }

    @PostMapping("/task/{taskId}/cancel")
    @LogRecord(value = "取消智能体任务", type = LogActionType.UPDATE)
    public CommonResMsgDTO<Boolean> cancelTask(@PathVariable String taskId,
                                               @RequestParam(required = false) String reason) {
        return agentClient.cancelTask(taskId, reason);
    }

    @GetMapping("/task/list")
    @LogRecord("查询会话任务列表")
    public CommonResMsgDTO<List<AgentTaskRet>> listTasksByChatId(@RequestParam Long chatId) {
        return agentClient.listTasksByChatId(chatId);
    }

    @GetMapping("/tools")
    @LogRecord("查询工具清单")
    public CommonResMsgDTO<ToolInventoryRet> listTools() {
        return agentClient.listTools();
    }

    @GetMapping("/credentials/status")
    @LogRecord("查询凭证冷却状态")
    public CommonResMsgDTO<Map<String, Object>> getCredentialStatus() {
        return agentClient.credentialStatus();
    }

    @PostMapping("/credentials/clear-cooldown")
    @LogRecord(value = "清除凭证冷却", type = LogActionType.UPDATE)
    public CommonResMsgDTO<Map<String, Object>> clearCredentialCooldown() {
        return agentClient.clearCredentialCooldown();
    }

    /**
     * SSE 流式订阅任务执行事件（透传 energy-ai-api processor）。
     */
    @GetMapping(value = "/task/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamTaskEvents(@PathVariable String taskId) {
        log.info("管理端订阅任务事件流: taskId={}", taskId);
        return agentClient.streamTaskEvents(taskId);
    }

    /**
     * 查询任务思考过程。
     */
    @GetMapping("/task/{taskId}/thoughts")
    @LogRecord("查询任务思考过程")
    public CommonResMsgDTO<List<Map<String, Object>>> getTaskThoughts(@PathVariable String taskId) {
        return agentClient.getTaskThoughts(taskId);
    }

    @GetMapping("/health")
    @LogRecord("AI 服务健康检查")
    public CommonResMsgDTO<Map<String, Object>> healthCheck() {
        return agentClient.healthCheck();
    }
}
