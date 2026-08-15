package com.endcy.ai.agent.orchestrator;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default expert agent registrar.
 *
 * <p>Registers skeletons for 3 domain expert agents (systemPrompt is a generic placeholder;
 * tool subsets await domain expert refinement). Users may override these prompts or refine
 * tool subsets in code.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultExpertRegistrar {

    private final SubAgentDispatcher subAgentDispatcher;

    @PostConstruct
    public void registerDefaults() {
        // Operations analysis expert
        subAgentDispatcher.registerExpert("operation", new SubAgentDispatcher.ExpertConfig(
                "operation",
                "运营分析专家",
                "你是运营数据分析专家，擅长：营收、利用率、业务量、故障率等运营指标的分析和报告。" +
                        "回答要数据驱动、结论清晰、有可执行建议。",
                List.of("searchKnowledgeBase", "callHttpApi")));

        // Fault diagnosis expert
        subAgentDispatcher.registerExpert("fault", new SubAgentDispatcher.ExpertConfig(
                "fault",
                "故障诊断专家",
                "你是设备故障诊断专家，擅长：故障码解读、设备状态分析、故障定位、应急处理方案、" +
                        "工单创建。回答要准确、有条理、优先给出安全相关建议。",
                List.of("searchKnowledgeBase")));

        // Order processing expert
        subAgentDispatcher.registerExpert("order", new SubAgentDispatcher.ExpertConfig(
                "order",
                "订单处理专家",
                "你是订单处理专家，擅长：订单查询、异常订单分析、退款/补偿判断、计费规则解释。" +
                        "回答要严谨、引用具体规则、给出明确处理建议。",
                List.of("searchKnowledgeBase", "callHttpApi")));

        log.info("默认专家 agent 注册完成: {}", subAgentDispatcher.listExperts());
    }
}
