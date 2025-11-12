package com.assistant.ai.agent;

import cn.hutool.core.util.StrUtil;
import com.assistant.ai.agent.model.IntentResult;
import com.assistant.service.domain.enums.KnowledgeBusinessTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

/**
 * 意图分析
 *
 * @author endcy
 * @date 2025/10/31 19:16:59
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntentAnalysisAgent {

    //理论上使用微调模型效果最佳
    private final ChatClient intentChatClient;

    public IntentResult analyzeIntent(Long chatId, String scopeType, String userMessage) {
        // 构建一个清晰的Prompt引导LLM分析意图
        StringBuilder typeTips = new StringBuilder();
        // 最佳实践是 从配置中获取意图分类，维护一个意图配置表；最好使用微调模型
        KnowledgeBusinessTypeEnum[] types = KnowledgeBusinessTypeEnum.values();
        for (KnowledgeBusinessTypeEnum type : types) {
            typeTips.append("- ").append(type.name()).append(": ").append(type.getDesc()).append("。 \n");
        }
        String promptTemplate = """
                你是智慧能源AI中的一个意图分析助手。请分析用户问题的意图类别。
                可选类别包括：
                """ + typeTips + """
                用户问题: %s
                请仅输出意图类别，不要输出其他内容。
                """;

        String prompt = String.format(promptTemplate, userMessage);
        ChatResponse chatResponse = intentChatClient.prompt().user(prompt).call().chatResponse();
        String intentCategory = null;
        if (chatResponse != null) {
            intentCategory = chatResponse.getResult().getOutput().getText();
            intentCategory = StrUtil.isBlank(intentCategory) ? KnowledgeBusinessTypeEnum.UNKNOWN.name() : intentCategory.trim();
        }
        log.info("chatId: {} now question intentCategory: {}", chatId, intentCategory);
        // 返回意图结果对象
        return new IntentResult(scopeType, intentCategory, chatId, userMessage);
    }

}
