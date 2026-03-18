package com.assistant.ai.advisor.retriever;

import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 使用语义向量相似度检索获取关联文档
 *
 * @author cxx641
 * @date 2025/12/4 14:16:44
 */
@Slf4j
public class VectorDocumentRetriever extends BaseDocumentRetriever {

    private final VectorStore vectorStore;


    public VectorDocumentRetriever(VectorStore vectorStore,
                                   ChatRagProperties chatRagProperties,
                                   DocumentQueryContext documentQueryContext) {
        super(documentQueryContext, chatRagProperties);
        this.vectorStore = vectorStore;
    }

    @NotNull
    @Override
    public List<Document> retrieve(@NotNull Query query) {
        Assert.notNull(query, "query cannot be null");
        var requestFilterExpression = computeRequestFilterExpression(query, VectorStoreDocumentRetriever.FILTER_EXPRESSION);
        SearchRequest searchRequest;
        if (requestFilterExpression != null) {
            searchRequest = SearchRequest.builder()
                                         .query(documentQueryContext.getReReadingQuestion())
                                         .filterExpression(requestFilterExpression)
                                         .similarityThreshold(chatRagProperties.getSimilarityThreshold())
                                         .topK(chatRagProperties.getSimilarityTopK())
                                         .build();
        } else {
            searchRequest = SearchRequest.builder()
                                         .query(documentQueryContext.getReReadingQuestion())
                                         .similarityThreshold(chatRagProperties.getSimilarityThreshold())
                                         .topK(chatRagProperties.getSimilarityTopK())
                                         .build();
        }
        return this.vectorStore.similaritySearch(searchRequest);
    }

}
