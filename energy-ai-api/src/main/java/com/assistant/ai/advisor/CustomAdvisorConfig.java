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
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ...
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
    //AI pg向量知识库
    private final PgVectorStore pgVectorVectorStore;
    private final MessageWindowChatMemory messageWindowChatMemory;

    @Bean("documentSimilarityAdvisor")
    public RetrievalAugmentationAdvisor documentSimilarityAdvisor() {
        //pg向量检索 相似度和召回量
        DocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                                                                  .vectorStore(pgVectorVectorStore)
                                                                  .similarityThreshold(chatRagProperties.getSimilarityThreshold())
                                                                  .topK(chatRagProperties.getSimilarityTopK())
                                                                  .build();
        // 检索增强顾问 允许空上下文，避免NPE
        return RetrievalAugmentationAdvisor.builder()
                                           .queryAugmenter(ContextualQueryAugmenter.builder()
                                                                                   .allowEmptyContext(true)
                                                                                   .build())
                                           .documentRetriever(retriever)
                                           .build();
    }

    @Bean("localDocumentAdvisor")
    public QuestionAnswerAdvisor localDocumentAdvisor(SimpleVectorStore localVectorStore) {
        return QuestionAnswerAdvisor.builder(localVectorStore).build();
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
            //自定义pg向量库
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
