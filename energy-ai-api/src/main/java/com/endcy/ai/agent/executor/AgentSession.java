package com.endcy.ai.agent.executor;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import org.springframework.ai.chat.messages.Message;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Persistent context for a single agent request (carried across steps).
 *
 * <p>Holds execution state, conversation history, token accounting, and the thought chain.</p>
 *
 * @author endcy
 * @since 2026-08-07
 */
@Data
public class AgentSession {

    private final String sessionId = IdUtil.fastSimpleUUID();

    /**
     * Business-side session ID (maps to existing chatId)
     */
    private Long chatId;

    private String userId;

    /**
     * Group ID (tenant/merchant/user group)
     */
    private String groupId;

    /**
     * Domain scope type (used for tool permission control)
     */
    private String scopeType;

    /**
     * User role (USER/OPERATOR/ADMIN, used for tool permission enhancement)
     */
    private String userRole;

    /**
     * Execution mode
     */
    private AgentMode mode = AgentMode.SINGLE_SHOT;

    /**
     * Current status
     */
    private AgentSessionStatus status = AgentSessionStatus.INITIALIZED;

    /**
     * Timestamps
     */
    private Instant startedAt = Instant.now();
    private Instant completedAt;

    /**
     * Cumulative tokens
     */
    private int totalPromptTokens;
    private int totalCompletionTokens;

    /**
     * Cumulative step count (AGENTIC / PLAN_AND_ACT mode)
     */
    private int currentStep;

    /**
     * Maximum step budget (null means no limit, relies on maxTokens / maxWallClockMs)
     */
    private Integer maxSteps;
    private Integer maxTokens;
    private Long maxWallClockMs;

    /**
     * Original request
     */
    private String userQuestion;

    /**
     * Final answer
     */
    private String finalAnswer;

    /**
     * Error message
     */
    private String errorMessage;

    /**
     * Thought chain (one record per step, used in AGENTIC mode)
     */
    private final List<AgentThought> thoughts = new ArrayList<>();

    /**
     * Conversation history context (loaded from ChatHistoryService)
     */
    private final List<Message> conversationHistory = new ArrayList<>();

    // ---- State transitions ----

    public void markRunning() {
        this.status = AgentSessionStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markCompleted(String answer) {
        this.status = AgentSessionStatus.COMPLETED;
        this.finalAnswer = answer;
        this.completedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = AgentSessionStatus.FAILED;
        this.errorMessage = error;
        this.completedAt = Instant.now();
    }

    public void markTerminatedByBudget(String reason) {
        this.status = AgentSessionStatus.TERMINATED_BY_BUDGET;
        this.errorMessage = "Budget exhausted: " + reason;
        this.completedAt = Instant.now();
    }

    /**
     * Budget check: returns true if any of step/token/time limit is exceeded.
     */
    public boolean isBudgetExceeded() {
        if (maxSteps != null && currentStep >= maxSteps) {
            return true;
        }
        if (maxTokens != null && (totalPromptTokens + totalCompletionTokens) >= maxTokens) {
            return true;
        }
        if (maxWallClockMs != null) {
            long elapsed = Instant.now().toEpochMilli() - startedAt.toEpochMilli();
            if (elapsed >= maxWallClockMs) {
                return true;
            }
        }
        return false;
    }

    public void recordTokens(int promptTokens, int completionTokens) {
        this.totalPromptTokens += promptTokens;
        this.totalCompletionTokens += completionTokens;
    }

    /**
     * 记录一步思考轨迹。
     * <p>注意：步数由执行器循环统一通过 {@link #setCurrentStep(int)} 管理，
     * 此处不再自增 currentStep，避免双重递增导致持久化步数偏大。</p>
     */
    public void recordThought(AgentThought thought) {
        this.thoughts.add(thought);
    }

    // ---- Inner class ----

    /**
     * Single-step thought record.
     */
    @Data
    public static class AgentThought {
        private int stepIndex;
        private String thought;
        /**
         * Tool calls list (JSON format)
         */
        private String toolCalls;
        /**
         * Tool execution results (JSON format)
         */
        private String toolResults;
        private long durationMs;
        private int promptTokens;
        private int completionTokens;
    }
}
