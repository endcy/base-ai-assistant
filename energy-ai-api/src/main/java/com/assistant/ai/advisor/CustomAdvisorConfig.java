package com.assistant.ai.advisor;

import cn.hutool.core.util.BooleanUtil;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.constant.EnergyAiConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义 Advisor 配置
 *
 * @author endcy
 * @date 2025/11/8 19:23:40
 */
@Configuration
@RequiredArgsConstructor
public class CustomAdvisorConfig {

    private final ChatRagProperties chatRagProperties;
    //AI 阿里云知识库 和向量库 二选一
    private final RetrievalAugmentationAdvisor aiRagCloudAdvisor;
    //AI pg 向量知识库
    private final PgVectorStore pgVectorVectorStore;
    private final MessageWindowChatMemory messageWindowChatMemory;

    @Bean("localDocumentAdvisor")
    public QuestionAnswerAdvisor localDocumentAdvisor(SimpleVectorStore localVectorStore) {
        return QuestionAnswerAdvisor.builder(localVectorStore)
                                    .promptTemplate(EnergyAiConstant.DEFAULT_PROMPT_TEMPLATE)
                                    .build();
    }

    /**
     * 业务库知识库 和 云文档知识库 二选一
     *
     * @return .
     */
    @Bean("storeDocumentAdvisor")
    public Advisor storeDocumentAdvisor() {
        //自定义云文档库 向量库和云端文档库 会有冲突，二选一
        if (BooleanUtil.isTrue(chatRagProperties.getEnableAliDashScopeIndex())) {
            return aiRagCloudAdvisor;
        } else {
            //自定义 pg 向量库
            return QuestionAnswerAdvisor.builder(pgVectorVectorStore).build();
        }
    }

    /**
     * 上下文查询增强器
     */
    @Bean("contextualQueryAugmenter")
    public ContextualQueryAugmenter contextualQueryAugmenter() {
        PromptTemplate emptyContextPromptTemplate = new PromptTemplate(EnergyAiConstant.PROMPT_TEMPLATE);
        return ContextualQueryAugmenter.builder()
                                       .allowEmptyContext(true)
                                       .emptyContextPromptTemplate(emptyContextPromptTemplate)
                                       .build();
    }

    @Bean("messageChatMemoryAdvisor")
    public MessageChatMemoryAdvisor messageChatMemoryAdvisor() {
        return MessageChatMemoryAdvisor.builder(messageWindowChatMemory).build();
    }

}
