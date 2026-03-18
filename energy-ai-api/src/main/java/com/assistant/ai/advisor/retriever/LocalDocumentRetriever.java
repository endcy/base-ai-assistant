package com.assistant.ai.advisor.retriever;

import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 本地文档 元数据 + 相似度检索获取关联文档
 *
 * @author cxx641
 * @date 2025/12/4 14:16:44
 */
@Slf4j
public class LocalDocumentRetriever extends BaseDocumentRetriever {
    public static final String LOCAL_FILTER_EXPRESSION = "qa_filter_expression";

    private final VectorStore vectorStore;

    public LocalDocumentRetriever(VectorStore vectorStore,
                                  ChatRagProperties chatRagProperties,
                                  DocumentQueryContext documentQueryContext) {
        super(documentQueryContext, chatRagProperties);
        this.vectorStore = vectorStore;
    }

    @NotNull
    @Override
    public List<Document> retrieve(@NotNull Query query) {
        Assert.notNull(query, "query cannot be null");

        var requestFilterExpression = computeRequestFilterExpression(query, LOCAL_FILTER_EXPRESSION);

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
