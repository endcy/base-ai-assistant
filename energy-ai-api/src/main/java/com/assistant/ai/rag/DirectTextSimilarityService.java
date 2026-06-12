package com.assistant.ai.rag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

/**
 * 直接文本相似度计算服务
 * 基于嵌入模型计算两段文本的余弦相似度
 *
 * @author endcy
 * @date 2025/12/4 11:26:12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DirectTextSimilarityService {

    // 嵌入模型
    private final EmbeddingModel dashscopeEmbeddingModel;

    // 计算两段文本的余弦相似度
    public double calculateSimilarity(String query, String content) {
        // 1. 将文本转换为向量（Embedding）
        float[] vector1 = convertTextToVector(query);
        float[] vector2 = convertTextToVector(content);

        // 2. 计算余弦相似度
        return cosineSimilarity(vector1, vector2);
    }

    private float[] convertTextToVector(String text) {
        // 调用嵌入模型获取文本的向量表示
        return dashscopeEmbeddingModel.embed(text);
    }

    // 余弦相似度计算算法
    private double cosineSimilarity(float[] vecA, float[] vecB) {
        if (vecA == null || vecB == null || vecA.length != vecB.length) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.length; i++) {
            dotProduct += vecA[i] * vecB[i];
            normA += Math.pow(vecA[i], 2);
            normB += Math.pow(vecB[i], 2);
        }

        double score = 0.0;
        if (normA == 0 || normB == 0) {
            return score;
        }
        try {
            score = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        } catch (Exception e) {
            log.warn("calc cosineSimilarity failed, {}", e.getMessage());
            return score;
        }

        return score;
    }
}
