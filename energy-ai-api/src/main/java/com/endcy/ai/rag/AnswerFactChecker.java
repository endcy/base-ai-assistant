package com.endcy.ai.rag;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 答案接地性校验器 —— 规避 AI 幻觉，保证答案有知识库依据。
 *
 * <p><b>三种校验策略</b>（按降级顺序）：</p>
 * <ol>
 *   <li><b>LLM-as-judge</b>（主）：检查 answer 中每句话是否能在 docs 中找到依据</li>
 *   <li><b>关键词回查</b>（兜底）：纯字符串包含匹配，覆盖范围有限但零成本</li>
 *   <li>无 docs 时直接放行（非 RAG 场景不校验）</li>
 * </ol>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerFactChecker {

    private static final String JUDGE_SYSTEM_PROMPT = """
            你是一个严格的事实核查员。给定【答案】和【参考资料】，逐句判断答案中每个事实陈述是否能在参考资料中找到依据。
            
            输出格式（严格 JSON，不要其他内容）：
            {
              "faithfulness": 0.0~1.0,
              "unsupported_claims": ["未支撑的陈述1", "..."],
              "grounded": true/false
            }
            """;

    private static final double FAITHFULNESS_THRESHOLD = 0.7;

    private final DashScopeChatModel dashscopeChatModel;

    /**
     * 校验答案接地性。
     *
     * @param answer    LLM 生成的最终答案
     * @param documents 检索到的相关文档（context）
     * @param question  用户原始问题
     * @return 校验结果
     */
    public FactCheckResult check(String answer, List<Document> documents, String question) {
        if (StrUtil.isBlank(answer)) {
            return FactCheckResult.pass("答案为空，跳过校验");
        }
        if (CollUtil.isEmpty(documents)) {
            return FactCheckResult.pass("无参考文档，非 RAG 场景，跳过校验");
        }

        // 答案长度 < 30 字符时用关键词回查（避免对小答案跑 LLM）
        if (answer.length() < 30) {
            return keywordCheck(answer, documents);
        }

        // LLM-as-judge
        try {
            return llmJudge(answer, documents, question);
        } catch (Exception e) {
            log.warn("LLM-as-judge 失败，降级为关键词回查: {}", e.getMessage());
            return keywordCheck(answer, documents);
        }
    }

    // ==================== LLM-as-judge ====================

    private FactCheckResult llmJudge(String answer, List<Document> documents, String question) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            context.append("[文档").append(i + 1).append("] ")
                   .append(documents.get(i).getText())
                   .append("\n");
        }

        String userPrompt = String.format("""
                        【参考资料】
                        %s
                        
                        【答案】
                        %s
                        
                        【用户原始问题】
                        %s
                        
                        请逐句核查答案中的事实陈述是否能在参考资料中找到依据。严格输出 JSON。""",
                context, answer, question);

        ChatResponse response = ChatClient.builder(dashscopeChatModel)
                                          .build()
                                          .prompt()
                                          .system(JUDGE_SYSTEM_PROMPT)
                                          .user(userPrompt)
                                          .call()
                                          .chatResponse();

        String text = response.getResult().getOutput().getText();
        return parseJudgeResult(text);
    }

    private FactCheckResult parseJudgeResult(String text) {
        if (StrUtil.isBlank(text)) {
            return FactCheckResult.pass("Judge 返回空，放行");
        }
        double faithfulness = extractDouble(text, "faithfulness", 1.0);
        boolean grounded = faithfulness >= FAITHFULNESS_THRESHOLD;
        List<String> unsupported = extractStringList(text, "unsupported_claims");

        if (grounded) {
            return FactCheckResult.pass(String.format("接地性 %.2f，通过", faithfulness));
        }
        return FactCheckResult.fail(String.format("接地性 %.2f，不通过。未支撑陈述: %s",
                faithfulness, unsupported), unsupported);
    }

    // ==================== 关键词回查（兜底） ====================

    private FactCheckResult keywordCheck(String answer, List<Document> documents) {
        StringBuilder allDocs = new StringBuilder();
        for (Document d : documents) {
            allDocs.append(d.getText()).append(" ");
        }
        String contextText = allDocs.toString();

        String[] sentences = answer.split("[。！？；,，.!?,;\\n]");
        List<String> unsupported = new ArrayList<>();
        int matched = 0;
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.length() < 4) {
                continue;
            }
            if (trimmed.length() >= 4 && contextText.contains(trimmed.substring(0, Math.min(6, trimmed.length())))) {
                matched++;
            } else {
                unsupported.add(trimmed);
            }
        }
        double faithfulness = sentences.length > 0 ? (double) matched / sentences.length : 1.0;
        boolean grounded = faithfulness >= FAITHFULNESS_THRESHOLD;
        if (grounded) {
            return FactCheckResult.pass(String.format("关键词回查接地性 %.2f，通过", faithfulness));
        }
        return FactCheckResult.fail(String.format("关键词回查接地性 %.2f，不通过", faithfulness), unsupported);
    }

    // ==================== 简易 JSON 解析 ====================

    private static double extractDouble(String json, String key, double defaultValue) {
        try {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx < 0) {
                return defaultValue;
            }
            int colonIdx = json.indexOf(":", idx);
            int commaOrBrace = Math.min(
                    json.indexOf(",", colonIdx) < 0 ? Integer.MAX_VALUE : json.indexOf(",", colonIdx),
                    json.indexOf("}", colonIdx) < 0 ? Integer.MAX_VALUE : json.indexOf("}", colonIdx));
            if (commaOrBrace == Integer.MAX_VALUE) {
                return defaultValue;
            }
            return Double.parseDouble(json.substring(colonIdx + 1, commaOrBrace).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static List<String> extractStringList(String json, String key) {
        List<String> result = new ArrayList<>();
        try {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx < 0) {
                return result;
            }
            int bracketStart = json.indexOf("[", idx);
            int bracketEnd = json.indexOf("]", bracketStart);
            if (bracketStart < 0 || bracketEnd < 0) {
                return result;
            }
            String arr = json.substring(bracketStart + 1, bracketEnd);
            for (String part : arr.split("\",\"")) {
                String cleaned = part.replaceAll("[\"\\[\\]]", "").trim();
                if (StrUtil.isNotBlank(cleaned)) {
                    result.add(cleaned);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    // ==================== 结果对象 ====================

    public record FactCheckResult(boolean grounded, String reason, List<String> unsupportedClaims) {
        public static FactCheckResult pass(String reason) {
            return new FactCheckResult(true, reason, List.of());
        }

        public static FactCheckResult fail(String reason, List<String> unsupported) {
            return new FactCheckResult(false, reason, unsupported != null ? unsupported : List.of());
        }
    }
}
