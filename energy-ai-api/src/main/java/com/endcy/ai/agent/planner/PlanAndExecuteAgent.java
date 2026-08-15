package com.endcy.ai.agent.planner;

import cn.hutool.core.collection.CollUtil;
import com.endcy.ai.agent.executor.AgentSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Plan-then-Execute agent.
 *
 * <p>First uses {@link PlanningAgent} to decompose a natural language request into a
 * {@link PlanningAgent.Plan}, then executes the plan step by step (currently sequential;
 * future versions may connect to a workflow DAG engine for branching/parallelism).</p>
 *
 * <p>Implementation entry point for
 * {@link com.endcy.ai.agent.executor.AgentMode#PLAN_AND_ACT} mode.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanAndExecuteAgent {

    private final PlanningAgent planningAgent;

    /**
     * Execute the Plan-then-Execute flow.
     *
     * @param session            session context
     * @param userRequest        user request
     * @param availableToolNames available tool name list (passed to planner)
     * @param stepExecutor       single-step execution callback (implemented by caller,
     *                           e.g. calling AgentExecutor or tools)
     * @return results of each step execution
     */
    public List<StepResult> execute(AgentSession session,
                                    String userRequest,
                                    List<String> availableToolNames,
                                    StepExecutor stepExecutor) {
        // 1. Plan
        PlanningAgent.Plan plan = planningAgent.plan(userRequest, availableToolNames);
        if (plan == null || CollUtil.isEmpty(plan.getSteps())) {
            log.warn("规划失败或无步骤，返回空结果 (sessionId={})", session.getSessionId());
            return List.of();
        }

        log.info("Plan-then-Execute 开始: {} 个步骤 (sessionId={})", plan.getSteps().size(), session.getSessionId());

        // 2. Execute step by step
        List<StepResult> results = new ArrayList<>();
        for (PlanningAgent.Step step : plan.getSteps()) {
            session.setCurrentStep(step.getId());
            if (session.isBudgetExceeded()) {
                log.warn("预算超限，终止 Plan-then-Execute at step {}", step.getId());
                break;
            }

            StepResult result;
            try {
                String output = stepExecutor.execute(step, session);
                result = new StepResult(step.getId(), StepResult.Status.SUCCESS, output, null);
            } catch (Exception e) {
                log.error("步骤 {} 执行失败: {}", step.getId(), e.getMessage());
                result = new StepResult(step.getId(), StepResult.Status.FAILED, null, e.getMessage());
                // Failure does not interrupt (fault-tolerant), subsequent steps continue
            }
            results.add(result);
            session.recordThought(buildThought(step, result));
        }

        return results;
    }

    private AgentSession.AgentThought buildThought(PlanningAgent.Step step, StepResult result) {
        AgentSession.AgentThought t = new AgentSession.AgentThought();
        t.setStepIndex(step.getId());
        t.setThought(step.getDescription());
        t.setToolCalls("[{\"name\":\"" + step.getTool() + "\",\"step\":\"" + step.getDescription() + "\"}]");
        t.setToolResults("[{\"output\":\"" + (result.output() != null ? result.output() : result.error()) + "\"}]");
        return t;
    }

    // ==================== Callback interface + results ====================

    /**
     * Single-step execution callback.
     */
    @FunctionalInterface
    public interface StepExecutor {
        String execute(PlanningAgent.Step step, AgentSession session);
    }

    /**
     * Step execution result.
     *
     * @param stepId step ID
     * @param status execution status
     * @param output output on success
     * @param error  error message on failure
     */
    public record StepResult(int stepId, Status status, String output, String error) {
        public enum Status {SUCCESS, FAILED}

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
    }
}
