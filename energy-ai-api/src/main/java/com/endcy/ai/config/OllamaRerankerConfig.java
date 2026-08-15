package com.endcy.ai.config;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankResponse;
import com.endcy.ai.rag.DirectTextSimilarityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

/**
 * Ollama 本地 Reranker 降级配置。
 *
 * <p>当 {@code ai.rag.enable-ollama-reranker=true} 时，用基于 Ollama embedding +
 * cosine similarity 的 RerankModel 替代 DashScope RerankModel。</p>
 *
 * <p>原理：对每个 candidate document 计算 query 与 content 的 embedding 余弦相似度，
 * 按得分降序排序。</p>
 *
 * @author endcy
 * @since 2026/08/10
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OllamaRerankerConfig {

    private final DirectTextSimilarityService directTextSimilarityService;

    /**
     * Ollama 降级 RerankModel —— 基于 embedding 余弦相似度的简易实现。
     *
     * <p>@Primary：启用时替换 DashScope RerankModel。
     * 注意：Ollama 未提供生产级 CrossEncoder 重排序，本实现用 embedding
     * 余弦相似度替代，适合低流量降级场景。</p>
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "ai.rag.enable-ollama-reranker", havingValue = "true")
    public RerankModel ollamaRerankModel() {
        log.info("OllamaRerankModel 已启用（基于 embedding cosine similarity）");
        return request -> {
            String query = request.getQuery();
            List<org.springframework.ai.document.Document> documents = request.getInstructions();
            if (CollUtil.isEmpty(documents)) {
                return new RerankResponse(List.of());
            }

            List<DocumentWithScore> results = new ArrayList<>(documents.size());
            for (org.springframework.ai.document.Document doc : documents) {
                String content = doc.getText();
                DocumentWithScore entry = DocumentWithScore.builder().build();
                entry.setDocument(doc);
                if (content == null) {
                    entry.setScore(0.0);
                    results.add(entry);
                    continue;
                }
                try {
                    double score = directTextSimilarityService.calculateSimilarity(query, content);
                    entry.setScore(score);
                } catch (Exception e) {
                    log.warn("Rerank 单文档失败 (id={}): {}", doc.getId(), e.getMessage());
                    entry.setScore(0.0);
                }
                results.add(entry);
            }
            results.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            return new RerankResponse(results);
        };
    }
}
