package com.assistant.ai.advisor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.model.RerankModel;
import com.assistant.ai.advisor.retriever.AdvisorRetrieverFactory;
import com.assistant.ai.advisor.retriever.BaseDocumentRetriever;
import com.assistant.ai.agent.model.IntentResult;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.constant.EnergyAiConstant;
import com.assistant.ai.domain.context.RequestRagContext;
import com.assistant.ai.rag.filter.VectorStoreFilterUtil;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.repository.domain.dto.KnowledgeDocumentDTO;
import com.assistant.ai.repository.service.VectorStoreService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * advisor 统一来源
 * 创建自定义的 RAG 检索增强顾问的工厂
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ChatClientAdvisorFactory {

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

    private final VectorStoreService vectorStoreService;

    private final ChatRagProperties chatRagProperties;
    //AI pg 文档重排序
    private final RerankModel rerankModel;

    private final PgVectorStore pgVectorVectorStore;

    private final MultiQueryExpander multiQueryExpander;

    private final AdvisorRetrieverFactory advisorRetrieverFactory;

    /**
     * 创建自定义的 RAG 检索增强顾问
     * pg 文档 元数据条件和相似度匹配
     *
     * @param documentParams 条件
     * @deprecated 使用 createHybridRetrievalAdvisor
     */
    @Deprecated
    public Advisor createRetrievalFilterAdvisor(KnowledgeDocumentDTO documentParams) {
        Map<String, Object> expressionParams = BeanUtil.beanToMap(documentParams, false, true);
        // 移除 expressionParams 中 value 为空的键值
        expressionParams.entrySet().removeIf(entry -> (entry.getValue() == null || StrUtil.isBlankIfStr(entry.getValue())));
        // 构建复杂的文档过滤条件
        Filter.Expression dynamicExpression = VectorStoreFilterUtil.buildFilterExpression(expressionParams);

        // 创建文档检索器
        DocumentRetriever documentRetriever;

        if (dynamicExpression != null) {
            documentRetriever = VectorStoreDocumentRetriever.builder()
                                                            .vectorStore(pgVectorVectorStore)
                                                            .filterExpression(dynamicExpression)
                                                            .similarityThreshold(chatRagProperties.getSimilarityThreshold())
                                                            .topK(chatRagProperties.getSimilarityTopK())
                                                            .build();
        } else {
            documentRetriever = VectorStoreDocumentRetriever.builder()
                                                            .vectorStore(pgVectorVectorStore)
                                                            .similarityThreshold(chatRagProperties.getSimilarityThreshold())
                                                            .topK(chatRagProperties.getSimilarityTopK())
                                                            .build();
        }
        return RetrievalAugmentationAdvisor.builder()
                                           .documentRetriever(documentRetriever)
                                           .queryAugmenter(contextualQueryAugmenter)
                                           .order(EnergyAiConstant.VECTOR_ADVISOR_ORDER)
                                           .build();
    }


    /**
     * bm25 关键词检索增强顾问
     * 支持元数据过滤条件
     *
     * @param documentParams 条件
     * @deprecated 使用 createHybridRetrievalAdvisor
     */
    @Deprecated
    public Advisor createBm25RetrievalAdvisor(DocumentQueryContext documentParams) {
        return Bm25RetrievalAdvisor.builder()
                                   .chatRagProperties(chatRagProperties)
                                   .vectorStoreService(vectorStoreService)
                                   .queryAugmenter(contextualQueryAugmenter)
                                   .documentParams(documentParams)
                                   .build();
    }

    /**
     * 包含了相似度检索的重排序 文档增强检索
     *
     * @param documentParams 元数据过滤条件
     * @deprecated 使用 createHybridRetrievalAdvisor
     */
    @Deprecated
    public HybridVectorRetrievalAdvisor createVectorHybridRetrievalAdvisor(DocumentQueryContext documentParams) {
        // 临时置空 原问题不作为元数据查询条件
        String originalQuestion = documentParams.getOriginalQuestion();
        documentParams.setOriginalQuestion(null);
        Map<String, Object> expressionParams = BeanUtil.beanToMap(documentParams, false, true);
        // 构建复杂的文档过滤条件
        Filter.Expression dynamicExpression = VectorStoreFilterUtil.buildFilterExpression(expressionParams);
        SearchRequest searchRequest;
        if (dynamicExpression != null) {
            searchRequest = SearchRequest.builder()
                                         .similarityThreshold(chatRagProperties.getSimilarityThreshold())
                                         .filterExpression(dynamicExpression)
                                         .topK(chatRagProperties.getSimilarityTopK())
                                         .build();
        } else {
            searchRequest = SearchRequest.builder()
                                         .similarityThreshold(chatRagProperties.getSimilarityThreshold())
                                         .topK(chatRagProperties.getSimilarityTopK())
                                         .build();
        }
        documentParams.setOriginalQuestion(originalQuestion);
        return new HybridVectorRetrievalAdvisor(pgVectorVectorStore, rerankModel, searchRequest, vectorStoreService, documentParams, chatRagProperties);
    }

    /**
     * 根据意图分析 创建各类文档检索器和重排序 检索增强
     *
     * @param documentParams    元数据过滤条件
     * @param intentResult      意图分析结果
     * @param requestRagContext 请求 RAG 上下文
     * @return 混合检索增强顾问
     */
    public HybridRetrievalAdvisor createHybridRetrievalAdvisor(DocumentQueryContext documentParams, IntentResult intentResult, RequestRagContext requestRagContext) {
        List<BaseDocumentRetriever> documentRetrievers = advisorRetrieverFactory.dynamicCreateRetrievers(documentParams, intentResult);
        return new HybridRetrievalAdvisor(rerankModel, chatRagProperties, documentRetrievers, multiQueryExpander, requestRagContext);
    }

}
