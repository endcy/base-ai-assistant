package com.assistant.ai.advisor;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import com.assistant.ai.config.ChatRagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义基于阿里云知识库服务的 RAG 增强顾问
 * 暂不使用，知识库空间需要不断上传资料，比本地或库管理更麻烦
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class AiRagCloudAdvisorConfig {

    private final DashScopeConnectionProperties dashScopeConnectionProperties;
    private final ChatRagProperties chatRagProperties;

    @Bean("aiRagCloudAdvisor")
    public RetrievalAugmentationAdvisor aiRagCloudAdvisor() {
//        if (!BooleanUtil.isTrue(chatRagProperties.getEnableAliDashScopeIndex())) {
//            return RetrievalAugmentationAdvisor.builder().build();
//        }
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                                                .apiKey(dashScopeConnectionProperties.getApiKey())
                                                .build();
        DocumentRetriever dashScopeDocumentRetriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                                                 .withIndexName(chatRagProperties.getAliDashScopeKnowledgeIndex())
                                                 .build());
        return RetrievalAugmentationAdvisor.builder()
                                           .documentRetriever(dashScopeDocumentRetriever)
                                           .build();
    }
}
