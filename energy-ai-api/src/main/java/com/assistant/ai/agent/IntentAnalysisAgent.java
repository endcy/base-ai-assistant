package com.assistant.ai.agent;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.assistant.ai.agent.model.IntentResult;
import com.assistant.ai.domain.enums.PossibleSourceTypeEnum;
import com.assistant.ai.repository.domain.dto.KnowledgeCategoryConfigDTO;
import com.assistant.ai.repository.service.KnowledgeCategoryConfigService;
import com.assistant.service.domain.enums.KnowledgeBusinessTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 意图分析
 * 完整实现：用户问题→业务分类→工具选择→数据来源预测
 *
 * @author endcy
 * @date 2025/10/31 19:16:59
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentAnalysisAgent {

    // 理论上使用微调模型效果最佳
    private final ChatClient intentChatClient;

    private final KnowledgeCategoryConfigService categoryConfigService;

    // "其他"类型的兜底配置
    private static final String OTHER_CATEGORY_CODE = KnowledgeBusinessTypeEnum.UNKNOWN.name();
    private static final String OTHER_CATEGORY_NAME = KnowledgeBusinessTypeEnum.UNKNOWN.getType();
    private static final String OTHER_CATEGORY_DESC = KnowledgeBusinessTypeEnum.UNKNOWN.getDesc();

    /**
     * 分析用户意图
     *
     * @param chatId      对话 ID
     * @param scopeType   知识领域类型
     * @param userMessage 用户问题
     * @return 意图分析结果
     */
    public IntentResult analyzeIntent(Long chatId, String scopeType, String userMessage) {
        // 从数据库获取业务分类配置
        List<KnowledgeCategoryConfigDTO> categoryConfigs = categoryConfigService.getByType("business");

        // 构建业务类型提示文本
        StringBuilder typeTips = new StringBuilder();
        for (KnowledgeCategoryConfigDTO config : categoryConfigs) {
            typeTips.append("- ").append(config.getCode())
                    .append("(").append(config.getName()).append("): ")
                    .append(StrUtil.isNotBlank(config.getDescription()) ? config.getDescription() : config.getName())
                    .append(". \n");
        }
        // 添加"其他"类型作为兜底
        if (categoryConfigs.stream().noneMatch(c -> c.getCode().equals(OTHER_CATEGORY_CODE))) {
            typeTips.append("- ").append(OTHER_CATEGORY_CODE)
                    .append("(").append(OTHER_CATEGORY_NAME).append("): ")
                    .append(OTHER_CATEGORY_DESC)
                    .append(". \n");
        }
        String promptTemplate = simpleIntentPromptTemplate(typeTips);

        String prompt = String.format(promptTemplate, userMessage);

        try {
            ChatResponse chatResponse = intentChatClient.prompt().user(prompt).call().chatResponse();
            if (chatResponse != null) {
                chatResponse.getResult();
                String responseText = chatResponse.getResult().getOutput().getText();
                log.info("chatId: {} raw intent analysis result: {}", chatId, responseText);

                // 尝试解析 响应
                IntentResult intentResult = parseIntentResponse(responseText, scopeType, chatId, userMessage);
                log.info("chatId: {} parsed intent result - businessType: {}, dataScopes: {}, tools: {}, confidence: {}",
                        chatId, intentResult.getBusinessType(), intentResult.getDataScopeList(),
                        intentResult.getRecommendedTools(), intentResult.getConfidence());
                return intentResult;
            }
        } catch (Exception e) {
            log.error("chatId: {} intent analysis error: {}", chatId, e.getMessage(), e);
        }

        // 返回默认结果
        return createDefaultIntentResult(scopeType, chatId, userMessage);
    }

    /**
     * 复杂意图，包含了业务分类、数据来源、工具推荐、置信度 按需拓展
     *
     * @param typeTips .
     * @return .
     */
    @NotNull
    private static String complexIntentPromptTemplate(StringBuilder typeTips) {
        // 数据来源类型说明
        String dataSourceTips = """
                - LOCAL: 本地文档，包含了特殊运维配置说明、代码、平台底层操作记录等文档
                - VECTOR: 数据库文档，包含了各类客服、售后、技术咨询、用户常见问题等文档数据
                - CLOUD: 在线云文档
                - DATABASE: 表数据，包含了放电订单、占位订单、会员订单、用户资金订单、站点信息、设备信息、各类计费策略信息等等相关的表内容数据
                - UNKNOWN: 未知，用户其他问题或非充电运营、非能源管理等相关的问题
                """;

        // 工具列表说明
        String toolTips = """
                - getOrderDetail: 查询订单详情
                - getUserInfo: 查询用户信息
                - getStationInfo: 查询站点信息
                - getDeviceStatus: 查询设备状态
                - searchKnowledge: 搜索知识库
                - webSearch: 网络搜索
                """;

        return """
                你是智慧能源 AI 中的一个意图分析专家。请分析用户问题的意图类别。
                
                1. **业务类型** (从以下类别中选择最匹配的一个):
                """ + typeTips + """
                
                2. **数据来源** (可多选，按优先级排序):
                """ + dataSourceTips + """
                
                3. **推荐工具** (可选择多个或空列表):
                """ + toolTips + """
                
                4. **置信度** (1-10 的整数，10 表示最确定)
                
                用户问题：%s
                
                请以 JSON 格式返回分析结果，格式如下:
                {
                    "businessType": "业务类型名称",
                    "dataScopes": ["数据来源 1", "数据来源 2"],
                    "recommendedTools": ["工具 1", "工具 2"],
                    "confidence": 8
                }
                """;
    }

    @NotNull
    private static String simpleIntentPromptTemplate(StringBuilder typeTips) {
        return """
                你是智慧能源AI中的一个意图分析助手。请分析用户问题的意图类别。
                可选类别包括：
                """ + typeTips + """
                用户问题: %s
                请仅输出意图类别，不要输出其他内容。
                """;
    }

    /**
     * 解析意图分析响应
     */
    private IntentResult parseIntentResponse(String responseText, String scopeType, Long chatId, String userMessage) {
        IntentResult result = new IntentResult();
        result.setScopeType(scopeType);
        result.setChatId(chatId);
        result.setUserMessage(userMessage);
        result.setDataScopeList(new ArrayList<>());
        result.setRecommendedTools(new ArrayList<>());
        result.setConfidence(5);

        // 尝试提取 JSON 内容
        String jsonStr = extractJsonFromResponse(responseText);
        if (StrUtil.isBlank(jsonStr)) {
            // 如果无法解析 JSON，则使用默认逻辑
            result.setBusinessType(OTHER_CATEGORY_CODE);
            return result;
        }
        // 简单的意图识别
        if (!JSONUtil.isTypeJSON(jsonStr)) {
            String intentCategory = jsonStr;
            intentCategory = StrUtil.isBlank(intentCategory) ? OTHER_CATEGORY_CODE : intentCategory.trim();
            result.setBusinessType(intentCategory);
            //加入平台默认的DB知识库
            result.addDataScope(PossibleSourceTypeEnum.VECTOR);
            return result;
        }

        // 复杂的意图识别解析，待按需拓展
        try {
            JSONObject json = new JSONObject(jsonStr);

            // 解析业务类型
            String businessType = json.optString("businessType", "");
            // 如果未指定业务类型，使用"其他"作为默认值
            result.setBusinessType(StrUtil.isNotBlank(businessType) ? businessType : OTHER_CATEGORY_CODE);

            // 解析数据来源
            List<String> dataScopes = json.optJSONArray("dataScopes") != null ?
                    json.getJSONArray("dataScopes").toList().stream().map(Object::toString).toList() :
                    new ArrayList<>();
            for (String scope : dataScopes) {
                PossibleSourceTypeEnum sourceType = PossibleSourceTypeEnum.create(scope.toUpperCase());
                if (sourceType != null) {
                    result.addDataScope(sourceType);
                }
            }
            // 如果没有指定数据来源，根据业务类型推断
            if (result.getDataScopeList() == null || result.getDataScopeList().isEmpty()) {
                result.addDataScope(inferDataScope(result.getBusinessType()));
            }

            // 解析推荐工具
            List<String> tools = json.optJSONArray("recommendedTools") != null ?
                    json.getJSONArray("recommendedTools").toList().stream().map(Object::toString).toList() :
                    new ArrayList<>();
            result.setRecommendedTools(tools);

            // 解析置信度
            result.setConfidence(json.optInt("confidence", 5));

        } catch (Exception e) {
            log.warn("chatId: {} failed to parse JSON response: {}", chatId, e.getMessage());
            return createDefaultIntentResult(scopeType, chatId, userMessage);
        }

        return result;
    }

    /**
     * 从响应中提取 JSON 内容
     */
    private String extractJsonFromResponse(String responseText) {
        if (StrUtil.isBlank(responseText)) {
            return null;
        }

        // 尝试找到 JSON 对象的开始和结束
        int start = responseText.indexOf('{');
        int end = responseText.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return responseText.substring(start, end + 1);
        }

        // 如果没有找到 JSON，返回原文本（可能是纯文本业务类型）
        return responseText.trim();
    }

    /**
     * 根据业务类型推断数据来源
     */
    private PossibleSourceTypeEnum inferDataScope(String businessType) {
        if (businessType == null) {
            return PossibleSourceTypeEnum.VECTOR;
        }

        return switch (businessType.toUpperCase()) {
            case "CHARGE_ORDER", "DISCHARGE_ORDER", "ACCOUNT" -> PossibleSourceTypeEnum.DATABASE;
            case "STATION", "EQUIPMENT" -> PossibleSourceTypeEnum.DATABASE;
            case "ALARM", "NORMS", "API", "PRODUCTION", "CLIENT_OPERATE", "ADMIN_OPERATE", "MAINTENANCE" -> PossibleSourceTypeEnum.VECTOR;
            case "REPORTER", "POWER_PREDICT" -> PossibleSourceTypeEnum.DATABASE;
            default -> PossibleSourceTypeEnum.VECTOR;
        };
    }

    /**
     * 创建默认意图结果
     */
    private IntentResult createDefaultIntentResult(String scopeType, Long chatId, String userMessage) {
        IntentResult result = new IntentResult();
        result.setScopeType(scopeType);
        result.setBusinessType(OTHER_CATEGORY_CODE);
        result.setChatId(chatId);
        result.setUserMessage(userMessage);
        result.setDataScopeList(List.of(PossibleSourceTypeEnum.VECTOR));
        result.setRecommendedTools(new ArrayList<>());
        result.setConfidence(3);
        return result;
    }
}
