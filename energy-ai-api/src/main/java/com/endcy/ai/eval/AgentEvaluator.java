package com.endcy.ai.eval;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent evaluator (LLM-as-judge).
 *
 * <p>Runs the agent against a set of Golden QAs and scores multi-dimensionally with LLM:
 * correctness / groundedness / completeness / tool call accuracy.</p>
 *
 * <p><b>Golden QA format</b> ({@code eval/golden/golden-qa.json}):</p>
 * <pre>
 *   [{"question":"...", "expectedKeywords":["..."], "expectedToolCalls":["..."]}]
 * </pre>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentEvaluator {

    private static final String JUDGE_PROMPT = """
            你是智能体回答质量评审员。给定【问题】【期望关键词】【实际回答】，打分。
            严格输出 JSON：{"correctness":0.0~1.0,"completeness":0.0~1.0,"groundedness":0.0~1.0,"passed":true/false,"reason":"..."}
            """;

    private final DashScopeChatModel dashscopeChatModel;

    /**
     * Evaluate a single QA.
     */
    public EvalResult evaluate(GoldenQA golden, String actualAnswer, List<String> actualToolCalls) {
        EvalResult result = new EvalResult();
        result.setQuestion(golden.getQuestion());

        // 1. Keyword hit rate (rule-based)
        double keywordHit = 0;
        if (CollUtil.isNotEmpty(golden.getExpectedKeywords()) && StrUtil.isNotBlank(actualAnswer)) {
            int hit = 0;
            for (String kw : golden.getExpectedKeywords()) {
                if (actualAnswer.contains(kw))
                    hit++;
            }
            keywordHit = (double) hit / golden.getExpectedKeywords().size();
        }
        result.setKeywordHitRate(keywordHit);

        // 2. Tool call accuracy (rule-based)
        if (CollUtil.isNotEmpty(golden.getExpectedToolCalls()) && actualToolCalls != null) {
            int hit = 0;
            for (String expected : golden.getExpectedToolCalls()) {
                if (actualToolCalls.contains(expected))
                    hit++;
            }
            result.setToolCallAccuracy((double) hit / golden.getExpectedToolCalls().size());
        }

        // 3. LLM-as-judge comprehensive scoring
        try {
            String userPrompt = String.format("""
                            【问题】%s
                            【期望关键词】%s
                            【实际回答】%s
                            请打分并输出 JSON。""", golden.getQuestion(),
                    golden.getExpectedKeywords(), StrUtil.maxLength(actualAnswer, 1000));

            BeanOutputConverter<LlmJudge> converter = new BeanOutputConverter<>(LlmJudge.class);
            LlmJudge judge = ChatClient.builder(dashscopeChatModel)
                                       .build()
                                       .prompt()
                                       .system(JUDGE_PROMPT)
                                       .user(u -> u.text(userPrompt).param("format", converter.getFormat()))
                                       .call()
                                       .entity(converter);
            if (judge != null) {
                result.setCorrectness(judge.getCorrectness());
                result.setCompleteness(judge.getCompleteness());
                result.setGroundedness(judge.getGroundedness());
                result.setPassed(Boolean.TRUE.equals(judge.getPassed()));
                result.setReason(judge.getReason());
            }
        } catch (Exception e) {
            log.warn("LLM-as-judge 失败，仅用规则评分: {}", e.getMessage());
            result.setPassed(keywordHit >= 0.5);
        }

        return result;
    }

    /**
     * Batch evaluation, returning a summary report.
     */
    public EvalReport evaluateAll(List<GoldenQA> goldens, AnswerProvider answerProvider) {
        List<EvalResult> results = new ArrayList<>();
        for (GoldenQA g : goldens) {
            try {
                String answer = answerProvider.getAnswer(g.getQuestion());
                EvalResult r = evaluate(g, answer, List.of());
                results.add(r);
            } catch (Exception e) {
                log.error("评估失败: {}", e.getMessage());
            }
        }
        return EvalReport.from(results);
    }

    // ==================== Data structures ====================

    @FunctionalInterface
    public interface AnswerProvider {
        String getAnswer(String question);
    }

    @lombok.Data
    public static class GoldenQA {
        private String question;
        private List<String> expectedKeywords;
        private List<String> expectedToolCalls;
    }

    @lombok.Data
    public static class LlmJudge {
        private Double correctness;
        private Double completeness;
        private Double groundedness;
        private Boolean passed;
        private String reason;
    }

    @lombok.Data
    public static class EvalResult {
        private String question;
        private double keywordHitRate;
        private double toolCallAccuracy;
        private Double correctness;
        private Double completeness;
        private Double groundedness;
        private boolean passed;
        private String reason;
    }

    public record EvalReport(int total, int passed, double passRate, double avgCorrectness,
                             double avgGroundedness, List<EvalResult> details) {
        public static EvalReport from(List<EvalResult> results) {
            int total = results.size();
            int pass = (int) results.stream().filter(EvalResult::isPassed).count();
            double avgCorrect = results.stream().filter(r -> r.getCorrectness() != null)
                                       .mapToDouble(EvalResult::getCorrectness).average().orElse(0);
            double avgGround = results.stream().filter(r -> r.getGroundedness() != null)
                                      .mapToDouble(EvalResult::getGroundedness).average().orElse(0);
            return new EvalReport(total, pass, total > 0 ? (double) pass / total : 0,
                    avgCorrect, avgGround, results);
        }
    }
}
