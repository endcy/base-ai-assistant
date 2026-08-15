package com.endcy.ai.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * AI RAG 配置。
 *
 * @author endcy
 * @date 2025/10/27
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.rag")
public class ChatRagProperties {

    private Double similarityThreshold = 0.6;

    private Integer similarityTopK = 5;

    private Double rerankMinScore = 0.1;

    private Double bm25SimilarityThreshold = 0.4;

    private Integer bm25TopK = 5;

    private Boolean enableLocalDocument = false;

    private String localDocumentPaths = "";

    private String resourceDocumentPath = "";

    private String aliDashScopeAppId = "";

    private String aliDashScopeKnowledgeIndex = "";

    private Boolean enableAliDashScopeIndex = false;

    private Boolean enableIntentAnalysis = false;

    private Boolean enableBm25Query = true;

    private Integer querySplitsWordNum = 512;

    private Boolean enableRetrieverLog = true;

    /**
     * 是否启用多媒体前置解析（图片/音频/视频 → 文本描述）
     */
    private Boolean enableMediaAnalysis = false;

    /**
     * 多媒体分析使用的模型名称（需支持多模态）
     */
    private String mediaAnalysisModel = "qwen3.5-omni-flash";

    // ============ Ollama 本地部署配置 ============

    /**
     * 是否启用 Ollama 本地 embedding 作为 DashScope embedding 的降级方案
     */
    private Boolean enableOllamaEmbedding = false;

    /**
     * Ollama 服务地址
     */
    private String ollamaBaseUrl = "http://localhost:11434";

    /**
     * Ollama embedding 模型名称
     */
    private String ollamaEmbeddingModel = "nomic-embed-text";

    /**
     * 是否启用 Ollama 本地 reranker 作为 DashScope rerank 的降级方案
     */
    private Boolean enableOllamaReranker = false;

    /**
     * Ollama reranker 使用的模型名称（复用 embedding 模型）
     */
    private String ollamaRerankerModel = "nomic-embed-text";

    /**
     * 捞取上下文对话最大记录数
     */
    private Integer maxContextRecords = 25;

    /**
     * 设置阿里 DashScope 知识库索引。
     * 注意：使用 ISO-8859-1 编码转换是因为 Apollo 配置中心在传输中文时会进行编码处理。
     */
    public void setAliDashScopeKnowledgeIndex(String value) {
        this.aliDashScopeKnowledgeIndex = StrUtil.isNotBlank(value) ? new String(value.getBytes(StandardCharsets.ISO_8859_1)) : value;
    }

}
