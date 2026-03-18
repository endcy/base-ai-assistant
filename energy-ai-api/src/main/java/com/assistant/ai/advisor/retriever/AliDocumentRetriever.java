package com.assistant.ai.advisor.retriever;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;

import java.util.List;

/**
 * 阿里云百炼空间云文档检索
 *
 * @author cxx641
 * @date 2025/12/4 16:49:24
 */
@Slf4j
public class AliDocumentRetriever extends BaseDocumentRetriever {
    private final DashScopeConnectionProperties dashScopeConnectionProperties;

    public AliDocumentRetriever(DashScopeConnectionProperties dashScopeConnectionProperties,
                                ChatRagProperties chatRagProperties,
                                DocumentQueryContext documentQueryContext) {
        super(documentQueryContext, chatRagProperties);
        this.dashScopeConnectionProperties = dashScopeConnectionProperties;
    }

    /**
     * 阿里百炼空间云文档
     *
     * @param query The query to use for retrieving documents .
     * @return .
     */
    @NotNull
    @Override
    public List<Document> retrieve(@NotNull Query query) {
        if (!BooleanUtil.isTrue(chatRagProperties.getEnableAliDashScopeIndex())) {
            return CollUtil.newArrayList();
        }
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                                                .apiKey(dashScopeConnectionProperties.getApiKey())
                                                .build();
        DashScopeDocumentRetrieverOptions options = DashScopeDocumentRetrieverOptions.builder()
                                                                                     .withIndexName(chatRagProperties.getAliDashScopeKnowledgeIndex())
                                                                                     .build();
        String pipelineId = dashScopeApi.getPipelineIdByName(options.getIndexName());
        if (pipelineId == null) {
            throw new DashScopeException("Index:" + options.getIndexName() + " NotExist");
        }
        return dashScopeApi.retriever(pipelineId, query.text(), options);
    }
}
