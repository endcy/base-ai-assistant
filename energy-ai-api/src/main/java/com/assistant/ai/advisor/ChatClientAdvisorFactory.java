package com.assistant.ai.advisor;

import cn.hutool.core.util.StrUtil;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.rag.filter.VectorStoreFilterUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * advisor 统一来源
 * 创建自定义的 RAG 检索增强顾问的工厂
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ChatClientAdvisorFactory {

    private final ChatRagProperties chatRagProperties;

    //文档相似度配置顾问
    @Getter
    private final RetrievalAugmentationAdvisor documentSimilarityAdvisor;
    //本地文档顾问
    @Getter
    private final QuestionAnswerAdvisor localDocumentAdvisor;
    //知识库向量知识库
    @Getter
    private final Advisor storeDocumentAdvisor;
    //日志顾问
    @Getter
    private final PromptLoggerAdvisor promptLoggerAdvisor;
    //查询重写顾问
    @Getter
    private final ReReadingAdvisor reReadingAdvisor;
    //消息记忆顾问
    @Getter
    private final MessageChatMemoryAdvisor messageChatMemoryAdvisor;

    private final ContextualQueryAugmenter contextualQueryAugmenter;

    /**
     * 创建自定义的 RAG 检索增强顾问
     *
     * @param expressionParams 条件
     * @return 自定义的 RAG 检索增强顾问
     */
    public Advisor createAiRagFilterAdvisor(Map<String, Object> expressionParams, VectorStore vectorStore) {
        // 移除expressionParams中value为空的键值
        expressionParams.entrySet().removeIf(entry -> (entry.getValue() == null || StrUtil.isBlankIfStr(entry.getValue())));
        // 构建复杂的文档过滤条件
        Filter.Expression dynamicExpression = VectorStoreFilterUtil.buildFilterExpression(expressionParams);

        // 创建文档检索器
        DocumentRetriever documentRetriever;
        if (dynamicExpression != null) {
            documentRetriever = VectorStoreDocumentRetriever.builder()
                                                            .vectorStore(vectorStore)
                                                            .filterExpression(dynamicExpression)
                                                            .similarityThreshold(chatRagProperties.getSimilarityThreshold())
                                                            .topK(chatRagProperties.getSimilarityTopK())
                                                            .build();
        } else {
            documentRetriever = VectorStoreDocumentRetriever.builder()
                                                            .vectorStore(vectorStore)
                                                            .similarityThreshold(chatRagProperties.getSimilarityThreshold())
                                                            .topK(chatRagProperties.getSimilarityTopK())
                                                            .build();
        }
        return RetrievalAugmentationAdvisor.builder()
                                           .documentRetriever(documentRetriever)
                                           .queryAugmenter(contextualQueryAugmenter)
                                           .build();
    }


}
