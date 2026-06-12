package com.assistant.ai.manager;

import cn.hutool.core.bean.BeanUtil;
import com.assistant.ai.advisor.retriever.VectorDocumentRetriever;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.rpc.domain.response.KnowledgeDocumentMatchItem;
import com.assistant.ai.util.DocumentConvertUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库文档匹配管理器
 * 基于向量检索进行文档相似度匹配
 *
 * @author endcy
 * @date 2025/12/16 18:00:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeDocumentManager {
    private final PgVectorStore pgVectorVectorStore;
    private final ChatRagProperties chatRagProperties;

    /**
     * 根据查询上下文进行文档匹配
     *
     * @param queryContext   文档查询上下文
     * @param similarityTopK 返回的最大匹配数量
     * @return 匹配的文档项列表
     */
    public List<KnowledgeDocumentMatchItem> match(DocumentQueryContext queryContext, Integer similarityTopK) {
        ChatRagProperties useConfig = BeanUtil.copyProperties(chatRagProperties, ChatRagProperties.class);
        useConfig.setSimilarityTopK(similarityTopK);
        Query originalQuery = Query.builder()
                                   .text(queryContext.getOriginalQuestion())
                                   .build();
        VectorDocumentRetriever retriever = new VectorDocumentRetriever(pgVectorVectorStore, useConfig, queryContext);

        List<Document> documents = retriever.retrieve(originalQuery);
        return DocumentConvertUtils.documentConvertRelated(documents);
    }
}
