package com.endcy.ai.agent.executor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.endcy.ai.agent.ToolCallAgent;
import com.endcy.ai.app.EnergyAiDocumentApp;
import com.endcy.ai.constant.EnergyAiConstant;
import com.endcy.ai.domain.context.RequestRagContext;
import com.endcy.ai.manager.ChatHistoryService;
import com.endcy.ai.manager.LbCredentialManager;
import com.endcy.ai.manager.ToolPermissionManager;
import com.endcy.ai.memory.MemorySummaryService;
import com.endcy.ai.rag.AnswerFactChecker;
import com.endcy.ai.repository.service.AgentSessionRepository;
import com.endcy.ai.rpc.domain.base.AIStreamResponse;
import com.endcy.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.endcy.ai.tools.registry.GuardedToolFactory;
import com.endcy.ai.tools.registry.ToolRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Default agent execution engine implementation.
 *
 * <p>Supported modes:
 * <ul>
 *   <li><b>SINGLE_SHOT</b>: delegates to {@code EnergyAiDocumentApp.doChatRag}</li>
 *   <li><b>AGENTIC</b>: manual think/act loop — each step LLM call → tool execution → reflection → continue,
 *       with maxSteps / budget control and last-round tool removal (inspired by Dify CotAgentRunner)</li>
 *   <li><b>PLAN_AND_ACT</b>: plan-first execution flow</li>
 * </ul>
 *
 * @author endcy
 * @since 2026-08-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAgentExecutor implements AgentExecutor {

    private final EnergyAiDocumentApp energyAiDocumentApp;
    private final LbCredentialManager lbCredentialManager;
    private final ChatClient commonChatClient;
    private final ToolCallback[] ragTools;
    private final AgentSessionRepository agentSessionRepository;
    private final GuardedToolFactory guardedToolFactory;
    private final ChatHistoryService chatHistoryService;
    private final MemorySummaryService memorySummaryService;
    private final AnswerFactChecker answerFactChecker;
    private final AgentEventPublisher agentEventPublisher;
    private final ToolPermissionManager toolPermissionManager;
    private final ToolRegistry toolRegistry;

    /**
     * AGENTIC mode default max steps.
     */
    private static final int DEFAULT_MAX_STEPS = 10;
    private static final String SCOPE_TYPE_DEFAULT = EnergyAiConstant.DEFAULT_SCOPE_TYPE;
    private static final String DEFAULT_TENANT_ID = EnergyAiConstant.DEFAULT_TENANT_ID;
    private static final String FACT_CHECK_WARNING = "警告：以上回答的部分内容未能在知识库中找到依据，请谨慎参考。";

    /**
     * Disable Spring AI ChatClient's built-in tool auto-execution — let the LLM response's
     * {@code assistant.getToolCalls()} return the tool call list, which this executor
     * executes manually and publishes events for. Follows the {@link ToolCallAgent} pattern.
     */
    private final ChatOptions agentChatOptions = DashScopeChatOptions.builder()
                                                                     .withInternalToolExecutionEnabled(false)
                                                                     .build();

    /**
     * Tool calling manager — responsible for correctly executing tools and returning
     * a message list containing {@link ToolResponseMessage}.
     */
    private final ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

    @Override
    public String execute(AgentSession session, String userQuestion) {
        session.markRunning();
        session.setUserQuestion(userQuestion);
        String taskId = session.getSessionId();
        try {
            String answer;
            switch (session.getMode()) {
                case SINGLE_SHOT:
                    answer = executeSingleShot(session);
                    break;
                case AGENTIC:
                    // Load history + summary compression
                    loadHistoryWithSummary(session);
                    answer = executeAgentic(session);
                    // Groundedness check
                    answer = applyFactCheck(answer, session);
                    break;
                case PLAN_AND_ACT:
                    loadHistoryWithSummary(session);
                    answer = executeAgentic(session);
                    answer = applyFactCheck(answer, session);
                    break;
                default:
                    throw new IllegalStateException("Unknown mode: " + session.getMode());
            }
            // Abnormal termination (budget/step limit): capture before markCompleted overwrites status
            boolean abnormal = session.getStatus() == AgentSessionStatus.TERMINATED_BY_BUDGET;
            session.markCompleted(answer);
            persistSessionCompletion(session);
            // Publish terminal event
            if (abnormal) {
                publishEvent(taskId, AgentEventPublisher.EventType.TASK_FAILED, session.getCurrentStep(),
                        "任务异常终止（预算/步数限制）", null, null, null);
            } else {
                publishEvent(taskId, AgentEventPublisher.EventType.TASK_COMPLETED, session.getCurrentStep(),
                        answer, null, null, null);
            }
            return answer;
        } catch (Exception e) {
            session.markFailed(e.getMessage());
            persistSessionCompletion(session);
            publishEvent(taskId, AgentEventPublisher.EventType.TASK_FAILED, session.getCurrentStep(),
                    e.getMessage(), null, null, null);
            log.error("AgentExecutor execute failed (sessionId={}): {}", session.getSessionId(), e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public Flux<String> executeStream(AgentSession session, String userQuestion) {
        session.markRunning();
        session.setUserQuestion(userQuestion);
        if (session.getMode() != AgentMode.SINGLE_SHOT) {
            return Flux.error(new UnsupportedOperationException(
                    "Streaming not yet supported for " + session.getMode()));
        }
        return executeSingleShotStream(session);
    }

    // ==================== SINGLE_SHOT ====================

    private String executeSingleShot(AgentSession session) {
        KnowledgeAIQueryParam param = buildParam(session);
        RequestRagContext ctx = new RequestRagContext();
        ctx.setChatId(session.getChatId());
        return energyAiDocumentApp.doChatRag(param, ctx);
    }

    private Flux<String> executeSingleShotStream(AgentSession session) {
        KnowledgeAIQueryParam param = buildParam(session);
        RequestRagContext ctx = new RequestRagContext();
        ctx.setChatId(session.getChatId());
        return energyAiDocumentApp.doChatRagStream(param, ctx)
                                  .filter(resp -> resp.getData() != null)
                                  .map(AIStreamResponse::getData);
    }

    // ==================== AGENTIC ====================

    /**
     * Explicit think/act loop.
     *
     * <p>Algorithm (inspired by Dify CotAgentRunner + last-round tool removal):</p>
     * <ol>
     *   <li>Initialize promptMessages: system + history + user question</li>
     *   <li>Loop until the LLM no longer calls tools or maxSteps is reached:
     *     <ul>
     *       <li>Call LLM (with tools)</li>
     *       <li>If it returns text (no tool call) → treat as final answer, return</li>
     *       <li>If it returns tool calls → execute tools, append results to history, continue</li>
     *       <li>Budget check → terminate early if exceeded</li>
     *     </ul>
     *   </li>
     *   <li>At maxSteps: remove the tool list and call LLM once more to force a final answer</li>
     * </ol>
     */
    private String executeAgentic(AgentSession session) {
        long startedMs = System.currentTimeMillis();
        int maxSteps = session.getMaxSteps() != null ? session.getMaxSteps() : DEFAULT_MAX_STEPS;
        String taskId = session.getSessionId();

        // Publish task start event
        publishEvent(taskId, AgentEventPublisher.EventType.TASK_STARTED, 0, null, null, null, null);

        // 1. Dynamically filter tools by scopeType + userRole
        List<String> allowedToolNames = toolPermissionManager.getToolsByPermission(
                session.getScopeType(), session.getUserRole());
        ToolCallback[] filteredRagTools = filterToolsByNames(mergeLocalAndMcpTools(), allowedToolNames);
        log.info("工具权限过滤：scopeType={}, userRole={}, allowedTools={}",
                session.getScopeType(), session.getUserRole(), allowedToolNames);

        // 2. Initialize message list - dynamically generate system prompt (only list authorized tools)
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(buildDynamicSystemPrompt(filteredRagTools)));
        messages.addAll(session.getConversationHistory());
        messages.add(new UserMessage(session.getUserQuestion()));

        String finalAnswer = "";

        for (int step = 1; step <= maxSteps; step++) {
            session.setCurrentStep(step);
            log.info("AGENTIC step {}/{} (sessionId={})", step, maxSteps, session.getSessionId());

            // Publish step start event
            publishEvent(taskId, AgentEventPublisher.EventType.STEP_STARTED, step, null, null, null, null);

            // Budget check
            if (session.isBudgetExceeded()) {
                session.markTerminatedByBudget("step=" + step);
                persistSessionCompletion(session);
                log.warn("Budget exceeded at step {} (sessionId={})", step, session.getSessionId());
                break;
            }

            // Last round removes tools (force LLM to produce text instead of more tool calls)
            boolean isLastStep = (step == maxSteps);
            ToolCallback[] toolsForThisStep = isLastStep ? new ToolCallback[0] :
                    guardedToolFactory.wrapWithGuards(filteredRagTools, session.getSessionId(),
                            session.getUserId(), session.getUserRole());

            long stepStartMs = System.currentTimeMillis();
            ChatResponse response;
            try {
                response = commonChatClient.prompt()
                                           .messages(messages)
                                           .toolCallbacks(toolsForThisStep)
                                           .options(agentChatOptions)
                                           .call()
                                           .chatResponse();
            } catch (Exception e) {
                // Rate limit / auth error → notify LbCredentialManager
                if (isRateLimitError(e)) {
                    lbCredentialManager.markRateLimited();
                } else if (isAuthError(e)) {
                    lbCredentialManager.markAuthError();
                }
                session.markFailed("LLM call error: " + e.getMessage());
                persistSessionCompletion(session);
                throw e;
            }
            long stepDurationMs = System.currentTimeMillis() - stepStartMs;

            // Record tokens
            int pt = 0, ct = 0;
            if ((response != null ? response.getMetadata() : null) != null && response.getMetadata().getUsage() != null) {
                pt = response.getMetadata().getUsage().getPromptTokens();
                ct = response.getMetadata().getUsage().getCompletionTokens();
                session.recordTokens(pt, ct);
            }

            AssistantMessage assistant = response != null ? response.getResult().getOutput() : null;

            // Publish thinking event
            publishEvent(taskId, AgentEventPublisher.EventType.THINKING, step, assistant != null ? assistant.getText() : null, null, null, null);

            // Record thought (in-memory + DB dual write)
            AgentSession.AgentThought thought = new AgentSession.AgentThought();
            thought.setStepIndex(step);
            thought.setThought(assistant != null ? assistant.getText() : null);
            thought.setDurationMs(stepDurationMs);
            thought.setPromptTokens(pt);
            thought.setCompletionTokens(ct);

            // Check for tool calls
            List<AssistantMessage.ToolCall> toolCalls = assistant != null ? assistant.getToolCalls() : null;
            if (CollUtil.isEmpty(toolCalls)) {
                // No tool calls → this is the final answer
                thought.setToolCalls("[]");
                thought.setToolResults("[]");
                session.recordThought(thought);
                persistThought(session.getSessionId(), thought);
                finalAnswer = (assistant != null ? assistant.getText() : null) != null ? assistant.getText() : "";

                // Publish final answer event
                publishEvent(taskId, AgentEventPublisher.EventType.FINAL_ANSWER, step, finalAnswer, null, null, null);

                log.info("AGENTIC final answer at step {} (sessionId={}, duration={}ms)",
                        step, session.getSessionId(), System.currentTimeMillis() - startedMs);
                return finalAnswer;
            }

            // Has tool calls → use ToolCallingManager for unified execution
            StringBuilder toolCallsJson = new StringBuilder("[");
            for (int i = 0; i < toolCalls.size(); i++) {
                AssistantMessage.ToolCall tc = toolCalls.get(i);
                // Publish tool call event
                publishEvent(taskId, AgentEventPublisher.EventType.TOOL_CALL, step, null, tc.name(), tc.arguments(), null);
                toolCallsJson.append("{\"name\":\"").append(tc.name())
                             .append("\",\"args\":").append(tc.arguments()).append("}");
                if (i < toolCalls.size() - 1)
                    toolCallsJson.append(",");
            }
            toolCallsJson.append("]");

            // Execute tools via ToolCallingManager — returns correct message list containing ToolResponseMessage
            Prompt currentPrompt = new Prompt(messages, agentChatOptions);
            List<Message> updatedHistory;
            try {
                ToolExecutionResult toolExecResult = toolCallingManager.executeToolCalls(currentPrompt, response);
                updatedHistory = toolExecResult.conversationHistory();
            } catch (Exception toolEx) {
                // Tool execution failure (e.g., unauthorized/nonexistent tool) —
                // do not abort; feed the error back as a tool result so the LLM decides the next step
                log.warn("工具执行失败，转为错误反馈继续 (sessionId={}): {}", session.getSessionId(), toolEx.getMessage());
                updatedHistory = buildToolErrorHistory(messages, assistant, toolEx);
            }

            // Extract each tool result from the last ToolResponseMessage, publish events and build JSON
            StringBuilder toolResultsJson = new StringBuilder("[");
            ToolResponseMessage toolRespMsg = (ToolResponseMessage) CollUtil.getLast(updatedHistory);
            int respIdx = 0;
            for (ToolResponseMessage.ToolResponse resp : toolRespMsg.getResponses()) {
                // Publish tool result event
                publishEvent(taskId, AgentEventPublisher.EventType.TOOL_RESULT, step, null, resp.name(), null, resp.responseData());
                toolResultsJson.append("{\"name\":\"").append(resp.name())
                               .append("\",\"result\":\"").append(escapeJson(resp.responseData())).append("\"}");
                if (respIdx < toolRespMsg.getResponses().size() - 1)
                    toolResultsJson.append(",");
                respIdx++;
            }
            toolResultsJson.append("]");

            thought.setToolCalls(toolCallsJson.toString());
            thought.setToolResults(toolResultsJson.toString());
            session.recordThought(thought);
            persistThought(session.getSessionId(), thought);

            // Update message context — ToolCallingManager already correctly handled assistant + tool response
            messages = new ArrayList<>(updatedHistory);
        }

        // Loop ended without final answer (step limit exhausted)
        log.warn("AGENTIC exhausted maxSteps={} without final answer (sessionId={})", maxSteps, session.getSessionId());
        if (session.getStatus() != AgentSessionStatus.TERMINATED_BY_BUDGET) {
            session.markTerminatedByBudget("maxSteps=" + maxSteps);
        }
        persistSessionCompletion(session);
        return StrUtil.blankToDefault(finalAnswer, "抱歉，思考步骤已达到上限，请稍后再试。");
    }

    /**
     * Publish an execution event.
     */
    private void publishEvent(String taskId, AgentEventPublisher.EventType type, int step,
                              String content, String toolName, String toolArgs, String toolResult) {
        AgentEventPublisher.AgentEvent event = AgentEventPublisher.AgentEvent.of(type, taskId);
        event.setStep(step);
        event.setContent(content);
        event.setToolName(toolName);
        event.setToolArgs(toolArgs);
        event.setToolResult(toolResult);
        agentEventPublisher.publish(event);
    }

    // ==================== Helpers ====================

    /**
     * Load history + summary compression (integrated with MemorySummaryService).
     */
    private void loadHistoryWithSummary(AgentSession session) {
        if (session.getChatId() == null)
            return;
        try {
            List<Message> history = chatHistoryService.loadHistoryFromDb(session.getChatId());
            List<Message> compressed = memorySummaryService.compressIfNeeded(history, session.getChatId());
            session.getConversationHistory().addAll(compressed);
            log.info("历史加载完成: {} 条消息 → {} 条 (sessionId={})",
                    history.size(), compressed.size(), session.getSessionId());
        } catch (Exception e) {
            log.warn("历史加载失败，继续无历史执行: {}", e.getMessage());
        }
    }

    /**
     * Groundedness check (integrated with AnswerFactChecker).
     * If the answer fails groundedness check, append a warning.
     */
    private String applyFactCheck(String answer, AgentSession session) {
        if (StrUtil.isBlank(answer))
            return answer;
        try {
            AnswerFactChecker.FactCheckResult result = answerFactChecker.check(answer, null, session.getUserQuestion());
            if (!result.grounded()) {
                log.warn("接地性校验未通过 (sessionId={}): {}", session.getSessionId(), result.reason());
                return answer + "\n\n" + FACT_CHECK_WARNING;
            }
            log.debug("接地性校验通过 (sessionId={})", session.getSessionId());
        } catch (Exception e) {
            log.warn("接地性校验异常，跳过: {}", e.getMessage());
        }
        return answer;
    }

    private KnowledgeAIQueryParam buildParam(AgentSession session) {
        KnowledgeAIQueryParam param = new KnowledgeAIQueryParam();
        param.setChatId(session.getChatId());
        param.setGroupId(session.getGroupId() != null ? session.getGroupId() : EnergyAiConstant.DEFAULT_TENANT_ID);
        param.setScopeType(SCOPE_TYPE_DEFAULT);
        param.setQueryType(EnergyAiConstant.RAG_QUERY_TYPE);
        param.setQuestion(session.getUserQuestion());
        return param;
    }

    private boolean isRateLimitError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("rate limit") || msg.contains("429");
    }

    private boolean isAuthError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("auth") || msg.contains("401") || msg.contains("403");
    }

    private String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * Dynamically generate the system prompt based on the current authorized tool set.
     * Only lists tools actually available for this request, preventing the LLM from
     * hallucinating calls to unauthorized/nonexistent tools.
     */
    private String buildDynamicSystemPrompt(ToolCallback[] tools) {
        StringBuilder toolList = new StringBuilder();
        if (tools != null && tools.length > 0) {
            toolList.append("\n当前可用工具（只能调用以下工具，未列出的工具一律不可调用）：\n");
            for (ToolCallback tc : tools) {
                if (tc.getToolDefinition() == null) {
                    continue;
                }
                String name = tc.getToolDefinition().name();
                String desc = tc.getToolDefinition().description();
                if (desc != null && desc.length() > 120) {
                    desc = desc.substring(0, 120) + "...";
                }
                toolList.append("- ").append(name);
                if (desc != null && !desc.isBlank()) {
                    toolList.append("：").append(desc.replace("\n", " ").trim());
                }
                toolList.append("\n");
            }
        } else {
            toolList.append("\n当前无可用工具。\n");
        }

        return """
                你是一个AI助手，拥有多种工具可以查询相关数据。\
                【强制规则】对于任何数据/事实/实时信息类问题，\
                你必须首先调用对应工具获取实时数据，再基于工具返回结果作答；\
                绝不允许在未调用工具的情况下仅凭记忆或推理回答数据类问题。\
                即使用户提到的日期/场景你认为可能无法精确满足（如未来日期），也应先调用工具尝试，\
                再如实把工具返回的结果告诉用户。不要反问用户"是否需要查询"，直接调用工具。\
                请严格只调用下面列出的工具，未列出的工具（如某查询功能未授权）不要尝试调用，\
                遇到无法用现有工具完成的需求，如实告知用户当前无对应工具或该功能未授权。\
                若某个工具调用失败，不要中断任务，基于已有信息继续回答，或如实告知用户该功能当前不可用。\
                回答要简洁、准确、结构清晰。""" + toolList;
    }

    /**
     * Build an error-feedback message list when tool execution fails (assistant tool calls + error tool response),
     * appended to message history so the LLM sees the failure reason and continues rather than the task failing.
     */
    private List<Message> buildToolErrorHistory(List<Message> messages, AssistantMessage assistant, Exception toolEx) {
        List<ToolResponseMessage.ToolResponse> errorResponses = new ArrayList<>();
        List<AssistantMessage.ToolCall> toolCalls = assistant != null ? assistant.getToolCalls() : null;

        String rawMsg = toolEx.getMessage() != null ? toolEx.getMessage() : toolEx.getClass().getSimpleName();
        String errMsg = "工具调用失败：" + rawMsg + "。请基于已有信息继续，或如实告知用户该功能当前不可用。";

        if (CollUtil.isNotEmpty(toolCalls)) {
            for (AssistantMessage.ToolCall tc : toolCalls) {
                errorResponses.add(new ToolResponseMessage.ToolResponse(tc.id(), tc.name(), errMsg));
            }
        } else {
            errorResponses.add(new ToolResponseMessage.ToolResponse("unknown", "unknown", errMsg));
        }

        ToolResponseMessage errorRespMsg = ToolResponseMessage.builder().responses(errorResponses).build();

        List<Message> updated = new ArrayList<>(messages);
        if (assistant != null) {
            updated.add(assistant);
        }
        updated.add(errorRespMsg);
        return updated;
    }

    private void persistSessionCompletion(AgentSession session) {
        agentSessionRepository.completeSession(
                session.getSessionId(),
                session.getStatus().name(),
                session.getFinalAnswer(),
                session.getErrorMessage(),
                session.getTotalPromptTokens(),
                session.getTotalCompletionTokens(),
                session.getCurrentStep());
    }

    private void persistThought(String sessionId, AgentSession.AgentThought thought) {
        agentSessionRepository.recordThought(
                sessionId,
                thought.getStepIndex(),
                thought.getThought(),
                thought.getToolCalls(),
                thought.getToolResults(),
                thought.getDurationMs(),
                thought.getPromptTokens(),
                thought.getCompletionTokens());
    }

    /**
     * Merge local ragTools with remote MCP tools registered in ToolRegistry (deduplicated, local first).
     */
    private ToolCallback[] mergeLocalAndMcpTools() {
        List<ToolCallback> merged = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        if (ragTools != null) {
            for (ToolCallback tc : ragTools) {
                if (tc != null && tc.getToolDefinition() != null && seen.add(tc.getToolDefinition().name())) {
                    merged.add(tc);
                }
            }
        }
        for (ToolCallback tc : toolRegistry.listAll()) {
            if (tc != null && tc.getToolDefinition() != null && seen.add(tc.getToolDefinition().name())) {
                merged.add(tc);
            }
        }
        return merged.toArray(new ToolCallback[0]);
    }

    private ToolCallback[] filterToolsByNames(ToolCallback[] allTools, List<String> allowedNames) {
        if (allowedNames == null || allowedNames.isEmpty()) {
            return allTools;
        }
        return java.util.Arrays.stream(allTools)
                               .filter(tc -> {
                                   String name = tc.getToolDefinition().name();
                                   return allowedNames.contains(name);
                               })
                               .toArray(ToolCallback[]::new);
    }
}
