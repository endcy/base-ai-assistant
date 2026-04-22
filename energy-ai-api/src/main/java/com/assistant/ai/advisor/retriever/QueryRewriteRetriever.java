package com.assistant.ai.advisor.retriever;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 查询改写检索器
 * 先用 LLM 将问题改写为 3 种不同表达，再分别做向量召回
 * 扩大语义覆盖面，结合 RRF 融合结果
 *
 * @author endcy
 * @date 2026/04/22
 */
@Slf4j
public class QueryRewriteRetriever extends BaseDocumentRetriever {

    private static final int REWRITE_COUNT = 3;
    private static final int RRF_K = 60;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public QueryRewriteRetriever(VectorStore vectorStore, ChatClient chatClient,
                                 ChatRagProperties chatRagProperties,
                                 DocumentQueryContext documentQueryContext) {
        super(documentQueryContext, chatRagProperties);
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
    }

    @Override
    public List<Document> retrieve(@NotNull Query query) {
        // 1. 用 LLM 改写为多种表达
        List<String> rewrittenQueries = rewriteQuery(query.text());
        if (CollUtil.isEmpty(rewrittenQueries)) {
            rewrittenQueries = List.of(query.text());
        }

        // 2. 对每个改写版本做向量检索
        Map<String, Double> rrfScores = new LinkedHashMap<>();
        Map<String, Document> keyToDocument = new LinkedHashMap<>();

        for (String rewrittenQuery : rewrittenQueries) {
            log.debug("QueryRewriteRetriever retrieving with: {}", rewrittenQuery);
            SearchRequest searchRequest = SearchRequest.builder()
                                                       .query(rewrittenQuery)
                                                       .similarityThreshold(chatRagProperties.getSimilarityThreshold())
                                                       .topK(chatRagProperties.getSimilarityTopK())
                                                       .filterExpression(computeRequestFilterExpression(query, "filter_expression"))
                                                       .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            accumulateRrfScores(results, rrfScores, keyToDocument);
        }

        // 3. 按 RRF 分数排序返回
        return rrfScores.entrySet().stream()
                        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                        .map(entry -> keyToDocument.get(entry.getKey()))
                        .collect(Collectors.toList());
    }

    /**
     * 使用 LLM 将问题改写为多种表达
     */
    private List<String> rewriteQuery(String originalQuery) {
        String prompt = """
                请将以下问题改写为 %d 种不同的表达方式，保持语义相同但用词不同。
                只输出改写后的问题列表，每行一个，不要有其他内容。
                
                原始问题：%s
                """.formatted(REWRITE_COUNT, originalQuery);

        try {
            String response = chatClient.prompt().user(prompt).call().content();
            if (StrUtil.isBlank(response)) {
                return List.of(originalQuery);
            }

            // 解析响应，每行一个改写
            List<String> queries = Arrays.stream(response.split("\n"))
                                         .map(String::trim)
                                         .filter(StrUtil::isNotBlank)
                                         .limit(REWRITE_COUNT)
                                         .toList();

            if (queries.isEmpty()) {
                return List.of(originalQuery);
            }

            log.debug("Query rewritten from '{}' to {} variants", originalQuery, queries.size());
            return queries;
        } catch (Exception e) {
            log.warn("Failed to rewrite query: {}", e.getMessage());
            return List.of(originalQuery);
        }
    }

    /**
     * 累积 RRF 分数
     * score(d) = Σ 1 / (k + rank)，k=60 为平滑常数
     */
    private void accumulateRrfScores(List<Document> results,
                                     Map<String, Double> rrfScores,
                                     Map<String, Document> keyToDocument) {
        for (int i = 0; i < results.size(); i++) {
            Document doc = results.get(i);
            String key = doc.getId();
            keyToDocument.put(key, doc);
            double score = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(key, score, Double::sum);
        }
    }
}
