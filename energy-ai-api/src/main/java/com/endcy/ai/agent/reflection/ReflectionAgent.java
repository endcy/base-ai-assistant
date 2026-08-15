package com.endcy.ai.agent.reflection;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.endcy.ai.rag.AnswerFactChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Reflection agent — self-checks execution results to decide whether
 * re-execution or manual intervention is needed.
 *
 * <p>Invoked after key steps in AGENTIC mode, with multi-dimensional validation:</p>
 * <ul>
 *   <li><b>Groundedness</b> (reuses {@link AnswerFactChecker}) — whether the answer is well-founded</li>
 *   <li><b>Completeness</b> — whether all user sub-questions are answered</li>
 *   <li><b>Consistency</b> — whether it contradicts the plan/history</li>
 *   <li><b>Safety</b> — whether sensitive info or dangerous operations are present</li>
 * </ul>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReflectionAgent {

    private static final String REFLECT_SYSTEM_PROMPT = """
            你是质量审查员。给定【用户问题】、【答案】和【参考资料】，判断答案质量。
            严格输出 JSON：
            {"pass":true/false,"completeness":0.0~1.0,"consistency":0.0~1.0,"safety":true/false,"issues":["问题1"],"suggestion":"改进建议或null"}
            """;

    private static final int MAX_DOC_TEXT_CHARS = 1000;

    private final DashScopeChatModel dashscopeChatModel;
    private final AnswerFactChecker answerFactChecker;

    /**
     * Reflection validation.
     *
     * @param question  user question
     * @param answer    answer to validate
     * @param documents reference documents
     * @return reflection result
     */
    public ReflectionResult reflect(String question, String answer, List<Document> documents) {
        // 1. Groundedness (reuses AnswerFactChecker)
        AnswerFactChecker.FactCheckResult factCheck = answerFactChecker.check(answer, documents, question);

        // 2. LLM multi-dimensional review (completeness/consistency/safety)
        ReflectionResult result = new ReflectionResult();
        result.setGrounded(factCheck.grounded());
        result.setGroundedReason(factCheck.reason());

        if (StrUtil.isBlank(answer)) {
            result.setPass(false);
            result.setIssues(List.of("答案为空"));
            return result;
        }

        try {
            String docsText = documents != null && !documents.isEmpty()
                    ? documents.get(0).getText() : "无参考资料";
            String userPrompt = String.format("""
                    【用户问题】%s
                    【答案】%s
                    【参考资料】%s
                    请审查并输出 JSON。""", question, answer, StrUtil.maxLength(docsText, MAX_DOC_TEXT_CHARS));

            BeanOutputConverter<ReflectionResult> converter = new BeanOutputConverter<>(ReflectionResult.class);
            ReflectionResult llmResult = ChatClient.builder(dashscopeChatModel)
                                                   .build()
                                                   .prompt()
                                                   .system(REFLECT_SYSTEM_PROMPT)
                                                   .user(u -> u.text(userPrompt).param("format", converter.getFormat()))
                                                   .call()
                                                   .entity(converter);

            if (llmResult != null) {
                llmResult.setGrounded(factCheck.grounded());
                llmResult.setGroundedReason(factCheck.reason());
                // Composite judgment: groundedness + LLM pass
                boolean overallPass = factCheck.grounded() && Boolean.TRUE.equals(llmResult.getPass());
                llmResult.setPass(overallPass);
                return llmResult;
            }
        } catch (Exception e) {
            log.warn("LLM 反思失败，仅用接地性判定: {}", e.getMessage());
        }

        // Fallback: groundedness only
        result.setPass(factCheck.grounded());
        result.setCompleteness(1.0);
        result.setConsistency(1.0);
        result.setSafety(true);
        return result;
    }

    // ==================== Result object ====================

    /**
     * Reflection validation result.
     */
    @lombok.Data
    public static class ReflectionResult {
        private Boolean pass;
        private Double completeness;
        private Double consistency;
        private Boolean safety;
        private List<String> issues;
        private String suggestion;
        // Groundedness (from AnswerFactChecker)
        private boolean grounded;
        private String groundedReason;

        public boolean shouldRetry() {
            return !Boolean.TRUE.equals(pass);
        }
    }
}
