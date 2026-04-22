package com.assistant.ai.advisor.retriever;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.assistant.ai.agent.model.IntentResult;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.domain.enums.PossibleSourceTypeEnum;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.repository.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 检索器聚合工厂
 * 根据意图分析结果动态创建检索器组合
 *
 * @author endcy
 * @date 2025/12/4 19:53:10
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AdvisorRetrieverFactory {

    private final PgVectorStore pgVectorVectorStore;
    private final SimpleVectorStore localVectorStore;
    private final ChatRagProperties chatRagProperties;
    private final VectorStoreService vectorStoreService;
    private final DashScopeConnectionProperties dashScopeConnectionProperties;
    private final ChatModel dashscopeChatModel;

    public List<BaseDocumentRetriever> dynamicCreateRetrievers(DocumentQueryContext documentParams, IntentResult intentResult) {
        List<BaseDocumentRetriever> documentRetrievers = CollUtil.newArrayList();
        List<PossibleSourceTypeEnum> dataScopeList = intentResult.getDataScopeList();
        if (dataScopeList == null) {
            dataScopeList = CollUtil.newArrayList();
        }
        for (PossibleSourceTypeEnum dataScope : dataScopeList) {
            switch (dataScope) {
                case UNKNOWN -> log.debug("无参考数据");
                case LOCAL -> documentRetrievers.add(new LocalDocumentRetriever(localVectorStore, chatRagProperties, documentParams));
                case VECTOR -> {
                    documentRetrievers.add(new VectorDocumentRetriever(pgVectorVectorStore, chatRagProperties, documentParams));
                    documentRetrievers.add(new Bm25DocumentRetriever(vectorStoreService, chatRagProperties, documentParams));
                    // 查询改写检索器：LLM 改写后多路召回 + RRF 融合
                    ChatClient rewriteChatClient = ChatClient.builder(dashscopeChatModel).build();
                    documentRetrievers.add(new QueryRewriteRetriever(pgVectorVectorStore, rewriteChatClient, chatRagProperties, documentParams));
                }
                case CLOUD -> documentRetrievers.add(new AliDocumentRetriever(dashScopeConnectionProperties, chatRagProperties, documentParams));
                default -> log.info("{} 无参考数据", dataScope);
            }
        }

        return documentRetrievers;
    }

}
