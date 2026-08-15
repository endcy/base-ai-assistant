package com.endcy.ai.controller;

import cn.hutool.core.util.StrUtil;
import com.endcy.ai.agent.executor.AgentMode;
import com.endcy.ai.agent.executor.AgentSession;
import com.endcy.ai.agent.executor.AgentStateMachine;
import com.endcy.ai.agent.executor.AgentTaskService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent task API — async task submission, status query, and cancellation.
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class AgentTaskController {

    private final AgentTaskService agentTaskService;

    @PostMapping
    public SubmitResponse submit(@RequestBody SubmitRequest req) {
        AgentMode mode = StrUtil.isNotBlank(req.getMode())
                ? AgentMode.valueOf(req.getMode().toUpperCase())
                : AgentMode.AGENTIC;
        String taskId = agentTaskService.submitTask(mode, req.getChatId(), req.getGroupId(), req.getQuestion(),
                req.getScopeType(), req.getUserRole());
        SubmitResponse resp = new SubmitResponse();
        resp.setTaskId(taskId);
        resp.setStatus("SUBMITTED");
        return resp;
    }

    @GetMapping("/{taskId}")
    public TaskStatusResponse getStatus(@PathVariable String taskId) {
        AgentSession session = agentTaskService.getTask(taskId);
        if (session == null) {
            return TaskStatusResponse.notFound(taskId);
        }
        return TaskStatusResponse.from(taskId, session);
    }

    @GetMapping
    public List<TaskStatusResponse> listAll() {
        return agentTaskService.listTasks().stream()
                               .map(s -> TaskStatusResponse.from(s.getSessionId(), s))
                               .toList();
    }

    @PostMapping("/{taskId}/cancel")
    public CancelResponse cancel(@PathVariable String taskId, @RequestParam(required = false) String reason) {
        boolean ok = agentTaskService.cancel(taskId, reason);
        CancelResponse resp = new CancelResponse();
        resp.setSuccess(ok);
        resp.setTaskId(taskId);
        return resp;
    }

    @Data
    public static class SubmitRequest {
        private String mode;
        private Long chatId;
        private String groupId;
        private String question;
        private String scopeType;
        private String userRole;
    }

    @Data
    public static class SubmitResponse {
        private String taskId;
        private String status;
    }

    @Data
    public static class TaskStatusResponse {
        private String taskId;
        private String status;
        private Integer currentStep;
        private Integer maxSteps;
        private String finalAnswer;
        private String errorMessage;
        private boolean terminal;

        public static TaskStatusResponse notFound(String taskId) {
            TaskStatusResponse r = new TaskStatusResponse();
            r.setTaskId(taskId);
            r.setStatus("NOT_FOUND");
            r.setTerminal(true);
            return r;
        }

        public static TaskStatusResponse from(String taskId, AgentSession s) {
            TaskStatusResponse r = new TaskStatusResponse();
            r.setTaskId(taskId);
            r.setStatus(s.getStatus().name());
            r.setCurrentStep(s.getCurrentStep());
            r.setMaxSteps(s.getMaxSteps());
            r.setFinalAnswer(s.getFinalAnswer());
            r.setErrorMessage(s.getErrorMessage());
            r.setTerminal(AgentStateMachine.isTerminal(s.getStatus()));
            return r;
        }
    }

    @Data
    public static class CancelResponse {
        private boolean success;
        private String taskId;
    }
}
