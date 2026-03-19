package com.assistant.ai.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * ai rag 配置
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

    public void setAliDashScopeKnowledgeIndex(String value) {
        this.aliDashScopeKnowledgeIndex = StrUtil.isNotBlank(value) ? new String(value.getBytes(StandardCharsets.ISO_8859_1)) : value;
    }

}
