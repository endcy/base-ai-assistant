package com.endcy.ai.prompt;

import com.endcy.ai.constant.EnergyAiConstant;
import lombok.Getter;

/**
 * Prompt template enumeration: uniformly manages all prompt logical names, file names,
 * and built-in constant fallbacks.
 *
 * <p>Fallback values come from {@link EnergyAiConstant}, ensuring zero-regression behavior
 * even if externalization is disabled or loading fails.</p>
 *
 * @author endcy
 * @since 2026-08-07
 */
@Getter
public enum PromptTemplateKey {

    SYSTEM("system", EnergyAiConstant.SYSTEM_PROMPT),
    OUT_OF_SCOPE("out-of-scope", EnergyAiConstant.PROMPT_TEMPLATE),
    INTENT_LEGACY("intent-legacy", EnergyAiConstant.INTENT_DETAIL_PROMPT_TEMPLATE),
    INTENT_SIMPLE("intent-simple", EnergyAiConstant.INTENT_SIMPLE_PROMPT_TEMPLATE),
    INTENT_COMPLEX_SYSTEM("intent-complex-system", EnergyAiConstant.INTENT_COMPLEX_PROMPT_TEMPLATE),
    INTENT_DETAIL("intent-detail", EnergyAiConstant.INTENT_DETAIL_PROMPT_TEMPLATE),
    RAG_DEFAULT("rag-default", ""),
    RAG_EMPTY("rag-empty", ""),
    RAG_RECOMMEND_QUESTION("rag-recommend-question", EnergyAiConstant.PROMPT_RAG_RECOMMEND_QUESTION_TEMPLATE),
    RAG_RECOMMEND_ANSWER("rag-recommend-answer", EnergyAiConstant.PROMPT_RAG_RECOMMEND_ANSWER_TEMPLATE),
    MEDIA_ANALYSIS_SYSTEM("media-analysis-system", EnergyAiConstant.MEDIA_ANALYSIS_SYSTEM_PROMPT),
    MEDIA_ANALYSIS_USER("media-analysis-user", EnergyAiConstant.MEDIA_ANALYSIS_USER_PROMPT);

    /**
     * File name under classpath:/prompts/ (without extension)
     */
    private final String fileName;

    /**
     * Built-in constant fallback (from EnergyAiConstant, ensuring zero regression)
     */
    private final String constantFallback;

    PromptTemplateKey(String fileName, String constantFallback) {
        this.fileName = fileName;
        this.constantFallback = constantFallback;
    }
}
