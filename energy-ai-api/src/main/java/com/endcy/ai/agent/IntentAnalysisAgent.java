package com.endcy.ai.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.endcy.ai.agent.model.IntentResponse;
import com.endcy.ai.agent.model.IntentResult;
import com.endcy.ai.prompt.PromptTemplateKey;
import com.endcy.ai.prompt.PromptTemplateService;
import com.endcy.ai.repository.domain.dto.KnowledgeCategoryConfigDTO;
import com.endcy.ai.repository.service.KnowledgeCategoryConfigService;
import com.endcy.service.domain.enums.KnowledgeBusinessTypeEnum;
import com.endcy.service.domain.enums.PossibleSourceTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Intent analysis agent.
 *
 * @author endcy
 * @date 2025/10/31 19:16:59
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentAnalysisAgent {

    // Ideally a fine-tuned model should be used
    private final ChatClient intentChatClient;

    private final KnowledgeCategoryConfigService knowledgeCategoryConfigService;

    private final PromptTemplateService promptTemplateService;

    private static final String BUSINESS_CATEGORY_TYPE = "business";

    // Fallback configuration for "Other" category
    private static final String OTHER_CATEGORY_CODE = KnowledgeBusinessTypeEnum.UNKNOWN.name();

    /**
     * Analyze user intent and return business classification result.
     *
     * @param chatId      conversation ID
     * @param scopeType   domain scope type
     * @param userMessage user message
     * @return intent analysis result
     */
    public IntentResult analyzeIntent(Long chatId, String scopeType, String userMessage) {
        // Get business category config from DB
        List<KnowledgeCategoryConfigDTO> categoryConfigs = knowledgeCategoryConfigService.getByType(BUSINESS_CATEGORY_TYPE);

        // Build business type hint text
        StringBuilder typeTips = new StringBuilder();
        for (KnowledgeCategoryConfigDTO config : categoryConfigs) {
            typeTips.append("- ").append(config.getCode())
                    .append(": ")
                    .append(StrUtil.isNotBlank(config.getDescription()) ? config.getDescription() : config.getName())
                    .append(". \n");
        }
        // Add "Other" category as fallback
        if (categoryConfigs.stream().noneMatch(c -> c.getCode().equals(OTHER_CATEGORY_CODE))) {
            typeTips.append("- ").append(OTHER_CATEGORY_CODE)
                    .append("(").append(KnowledgeBusinessTypeEnum.UNKNOWN.getType()).append("): ")
                    .append(KnowledgeBusinessTypeEnum.UNKNOWN.getDesc())
                    .append(". \n");
        }

        String prompt = String.format(promptTemplateService.getTemplate(PromptTemplateKey.INTENT_SIMPLE), typeTips, userMessage);
        ChatResponse chatResponse = intentChatClient.prompt().user(prompt).call().chatResponse();
        String intentCategory = null;
        if (chatResponse != null) {
            intentCategory = chatResponse.getResult().getOutput().getText();
            intentCategory = StrUtil.isBlank(intentCategory) ? OTHER_CATEGORY_CODE : intentCategory.trim();
        }

        log.info("chatId: {} now question intentCategory: {}", chatId, intentCategory);
        IntentResult ret = new IntentResult();
        ret.setChatId(chatId);
        ret.setUserMessage(userMessage);
        ret.setScopeType(scopeType);
        ret.setBusinessType(intentCategory);
        // Return intent result object
        return ret;
    }

    public IntentResult analyzeQuestionV0(Long chatId, String scopeType, String userMessage) {
        // Get business category config from DB
        List<KnowledgeCategoryConfigDTO> categoryConfigs = knowledgeCategoryConfigService.getByType(BUSINESS_CATEGORY_TYPE);

        StringBuilder intentCategoryTypeTips = new StringBuilder();
        for (KnowledgeCategoryConfigDTO config : categoryConfigs) {
            intentCategoryTypeTips.append("- ").append(config.getCode())
                                  .append(": ")
                                  .append(StrUtil.isNotBlank(config.getDescription()) ? config.getDescription() : config.getName())
                                  .append(". \n");
        }
        // Add "Other" category as fallback
        if (categoryConfigs.stream().noneMatch(c -> c.getCode().equals(OTHER_CATEGORY_CODE))) {
            intentCategoryTypeTips.append("- ").append(OTHER_CATEGORY_CODE)
                                  .append("(").append(KnowledgeBusinessTypeEnum.UNKNOWN.getType()).append("): ")
                                  .append(KnowledgeBusinessTypeEnum.UNKNOWN.getDesc())
                                  .append(". \n");
        }

        StringBuilder possibleSourceTypeTips = new StringBuilder();
        // Best practice: get intent classification from config, maintain an intent config table; ideally use a fine-tuned model
        PossibleSourceTypeEnum[] possibleSourceTypes = PossibleSourceTypeEnum.values();
        for (PossibleSourceTypeEnum type : possibleSourceTypes) {
            possibleSourceTypeTips.append("- ").append(type.name()).append(": ").append(type.getDesc()).append("。 \n");
        }

        IntentResult ret = new IntentResult();
        ret.setChatId(chatId);
        ret.setUserMessage(userMessage);
        ret.setScopeType(scopeType);
        String prompt = String.format(promptTemplateService.getTemplate(PromptTemplateKey.INTENT_LEGACY), intentCategoryTypeTips, possibleSourceTypeTips, userMessage, scopeType);
        ChatResponse chatResponse = null;
        try {
            chatResponse = intentChatClient.prompt().user(prompt).call().chatResponse();
        } catch (Exception e) {
            log.error("chatId: {} analyzeQuestion error: {}", chatId, e.getMessage());
            return ret;
        }

        if (chatResponse == null) {
            return ret;
        }
        String responseTypes = chatResponse.getResult().getOutput().getText();
        if (StrUtil.isBlank(responseTypes)) {
            return ret;
        }
        responseTypes = responseTypes.trim();
        String[] responseLines = responseTypes.split("\n");

        String intentCategory = null;
        if (responseLines.length > 0 && StrUtil.isNotBlank(responseLines[0].trim())) {
            intentCategory = responseLines[0].trim();
            intentCategory = StrUtil.isBlank(intentCategory) ? OTHER_CATEGORY_CODE : intentCategory;
        }
        ret.setBusinessType(intentCategory);

        List<PossibleSourceTypeEnum> dataScopeList = CollUtil.newArrayList();
        if (responseLines.length > 0 && StrUtil.isNotBlank(responseLines[1].trim())) {
            String[] intentScopes = responseLines[1].trim().split(",");
            for (String intentScope : intentScopes) {
                PossibleSourceTypeEnum possibleSourceTypeEnum = PossibleSourceTypeEnum.create(intentScope);
                if (possibleSourceTypeEnum != null) {
                    dataScopeList.add(possibleSourceTypeEnum);
                }
            }
        }
        ret.setDataScopeList(dataScopeList);

        log.info("chatId: {} now question intentCategory: {} possible dataScopes：{}", chatId, intentCategory, dataScopeList);
        return ret;
    }

    /**
     * Analyze user question (new structured output), return intent result.
     *
     * @param chatId      conversation ID
     * @param scopeType   domain scope type
     * @param userMessage user message
     * @return intent analysis result (includes business classification and data sources)
     */
    public IntentResult analyzeQuestion(Long chatId, String scopeType, String userMessage) {
        // Initialize return result
        IntentResult ret = new IntentResult();
        ret.setChatId(chatId);
        ret.setUserMessage(userMessage);
        ret.setScopeType(scopeType);
        // Fallback output
        ret.setBusinessType(OTHER_CATEGORY_CODE);
        ret.setDataScopeList(Collections.emptyList());
        try {
            // 1. Get business category config
            List<KnowledgeCategoryConfigDTO> categoryConfigs = knowledgeCategoryConfigService.getByType(BUSINESS_CATEGORY_TYPE);

            // Build intent classification hints
            StringBuilder intentCategoryTips = new StringBuilder();
            for (KnowledgeCategoryConfigDTO config : categoryConfigs) {
                intentCategoryTips.append(config.getName())
                                  .append(": ")
                                  .append(StrUtil.blankToDefault(config.getDescription(), config.getName()))
                                  .append("\\n");
            }

            // Add "Other" category as fallback
            if (categoryConfigs.stream().noneMatch(c -> OTHER_CATEGORY_CODE.equals(c.getCode()))) {
                intentCategoryTips.append(OTHER_CATEGORY_CODE)
                                  .append("(").append(KnowledgeBusinessTypeEnum.UNKNOWN.getType()).append("): ")
                                  .append(KnowledgeBusinessTypeEnum.UNKNOWN.getDesc())
                                  .append("\\n");
            }

            // 2. Build data source hints
            StringBuilder sourceTypeTips = new StringBuilder();
            PossibleSourceTypeEnum[] possibleSourceTypes = PossibleSourceTypeEnum.values();
            for (PossibleSourceTypeEnum type : possibleSourceTypes) {
                sourceTypeTips.append(type.name()).append(": ").append(type.getDesc()).append("\\n");
            }

            // 3. Create output converter
            BeanOutputConverter<IntentResponse> converter = new BeanOutputConverter<>(IntentResponse.class);

            String systemPrompt = buildSystemPrompt();
            String userPrompt = buildUserPrompt(userMessage, scopeType, intentCategoryTips.toString(), sourceTypeTips.toString());
            // 4. Use ChatClient's fluent API, automatically handle format instructions
            IntentResponse intentResponse = intentChatClient.prompt()
                                                            .system(systemPrompt)
                                                            .user(userSpec -> userSpec
                                                                    .text(userPrompt)
                                                                    .param("format", converter.getFormat())
                                                            )
                                                            .call()
                                                            .entity(converter);


            // 5. Process analysis result
            if (intentResponse != null) {
                processIntentResponse(intentResponse, ret, chatId);
            }
        } catch (Exception e) {
            log.error("chatId: {} 意图分析失败: {}", chatId, e.getMessage(), e);
        }

        return ret;
    }

    /**
     * Build system prompt to force LLM to output in structured format.
     */
    private String buildSystemPrompt() {
        return promptTemplateService.getTemplate(PromptTemplateKey.INTENT_COMPLEX_SYSTEM);
    }

    /**
     * Build user prompt.
     */
    private String buildUserPrompt(String userMessage, String scopeType, String intentCategoryTips, String sourceTypeTips) {
        return String.format(promptTemplateService.getTemplate(PromptTemplateKey.INTENT_DETAIL), userMessage, scopeType, intentCategoryTips, sourceTypeTips);
    }

    /**
     * Process intent response result.
     */
    private void processIntentResponse(IntentResponse intentResponse, IntentResult ret, Long chatId) {
        // Process intent classification
        String intentCategory = StrUtil.trimToEmpty(intentResponse.getBusinessType());
        if (StrUtil.isBlank(intentCategory)) {
            intentCategory = OTHER_CATEGORY_CODE;
        }
        ret.setBusinessType(intentCategory);

        // Process data sources
        List<PossibleSourceTypeEnum> dataScopeList = CollUtil.newArrayList();
        if (CollUtil.isNotEmpty(intentResponse.getScopes())) {
            for (String scope : intentResponse.getScopes()) {
                if (StrUtil.isNotBlank(scope)) {
                    PossibleSourceTypeEnum sourceType = PossibleSourceTypeEnum.create(scope.trim());
                    if (sourceType != null && !dataScopeList.contains(sourceType)) {
                        dataScopeList.add(sourceType);
                    }
                }
            }
        }
        ret.setDataScopeList(dataScopeList);

        log.info("chatId: {} 意图分析完成 - 分类: {}, 数据来源: {}", chatId, intentCategory, dataScopeList);
    }

}
