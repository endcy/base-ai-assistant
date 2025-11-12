package com.assistant.ai.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * ai rag配置
 *
 * @author endcy
 * @date 2025/10/27
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.rag")
public class ChatRagProperties {

    private Double similarityThreshold = 0.6;

    private Integer similarityTopK = 3;

    private Boolean enableLocalDocument = false;

    private String localDocumentPaths = "";

    private String resourceDocumentPath = "";

    private String aliDashScopeAppId = "";

    private String aliDashScopeKnowledgeIndex = "";

    private Boolean enableAliDashScopeIndex = false;

    private Boolean enableIntentAnalysis = false;

    public void setAliDashScopeKnowledgeIndex(String value) {
        this.aliDashScopeKnowledgeIndex = StrUtil.isNotBlank(value) ? new String(value.getBytes(StandardCharsets.ISO_8859_1)) : value;
    }

}
