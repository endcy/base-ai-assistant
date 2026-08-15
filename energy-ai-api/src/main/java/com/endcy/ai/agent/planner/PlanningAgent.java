package com.endcy.ai.agent.planner;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Planning agent — decomposes a complex user request into a structured execution plan.
 *
 * <p>Before AGENTIC mode execution, the LLM generates a {@link Plan}, which is then executed
 * step by step (Plan-then-Execute pattern).</p>
 *
 * <p><b>Usage</b>:</p>
 * <pre>
 *   Plan plan = planningAgent.plan("帮我查最近一周所有站点的故障并生成报告", List.of("queryFault", "generateReport"));
 *   for (Plan.Step s : plan.getSteps()) {
 *       // execute each step
 *   }
 * </pre>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlanningAgent {

    private static final String PLAN_SYSTEM_PROMPT = """
            你是任务规划专家。把用户的复杂请求分解为 1-5 个可执行的步骤。
            每个步骤必须明确：描述、预期使用的工具名（如有）、预期输出。
            如果请求简单（单步可完成），只输出 1 个步骤。
            严格输出 JSON，不要其他内容。
            """;

    private final DashScopeChatModel dashscopeChatModel;

    /**
     * Generate an execution plan.
     *
     * @param userRequest        user request
     * @param availableToolNames available tool names
     * @return execution plan
     */
    public Plan plan(String userRequest, List<String> availableToolNames) {
        String toolsHint = availableToolNames != null ? String.join(", ", availableToolNames) : "无";
        String userPrompt = String.format("""
                用户请求：%s
                
                可用工具：%s
                
                请分解为执行步骤，输出 JSON：
                {"steps":[{"id":1,"description":"步骤描述","tool":"工具名或null","expectedOutput":"预期输出"}]}
                """, userRequest, toolsHint);

        BeanOutputConverter<Plan> converter = new BeanOutputConverter<>(Plan.class);
        try {
            Plan plan = ChatClient.builder(dashscopeChatModel)
                                  .build()
                                  .prompt()
                                  .system(PLAN_SYSTEM_PROMPT)
                                  .user(u -> u.text(userPrompt).param("format", converter.getFormat()))
                                  .call()
                                  .entity(converter);
            log.info("规划完成：{} 个步骤 (request={})", plan != null && plan.getSteps() != null ? plan.getSteps().size() : 0,
                    StrUtil.maxLength(userRequest, 50));
            return plan;
        } catch (Exception e) {
            log.warn("规划失败，降级为单步: {}", e.getMessage());
            return Plan.singleStep(userRequest);
        }
    }

    // ==================== Plan data structure ====================

    /**
     * Execution plan, containing a list of steps.
     */
    @lombok.Data
    public static class Plan {
        private List<Step> steps;

        /**
         * Create a single-step plan (degradation fallback).
         *
         * @param description step description
         * @return plan containing a single step
         */
        public static Plan singleStep(String description) {
            Step s = new Step();
            s.setId(1);
            s.setDescription(description);
            s.setTool(null);
            s.setExpectedOutput("直接回答");
            Plan p = new Plan();
            p.setSteps(List.of(s));
            return p;
        }
    }

    /**
     * Single execution step.
     */
    @lombok.Data
    public static class Step {
        private int id;
        private String description;
        private String tool;
        private String expectedOutput;
    }
}
