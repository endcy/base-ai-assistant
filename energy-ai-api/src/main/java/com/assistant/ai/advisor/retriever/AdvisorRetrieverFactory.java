package com.assistant.ai.advisor.retriever;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.assistant.ai.agent.model.IntentResult;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.domain.enums.PossibleSourceTypeEnum;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.repository.service.VectorStoreService;
import com.assistant.service.common.constant.BusinessConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 检索器聚合工厂
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
                    // 在租户知识以外 针对平台级知识是共享的
                    if (documentParams.getGroupId() != null && !documentParams.getGroupId().equals(BusinessConstant.PLATFORM_GROUP_ID)) {
                        documentParams.setGroupId(BusinessConstant.PLATFORM_GROUP_ID);
                        documentRetrievers.add(new VectorDocumentRetriever(pgVectorVectorStore, chatRagProperties, documentParams));
                    }
                    documentRetrievers.add(new VectorDocumentRetriever(pgVectorVectorStore, chatRagProperties, documentParams));
                    documentRetrievers.add(new Bm25DocumentRetriever(vectorStoreService, chatRagProperties, documentParams));
                }
                case CLOUD -> documentRetrievers.add(new AliDocumentRetriever(dashScopeConnectionProperties, chatRagProperties, documentParams));
                default -> log.info("{} 无参考数据", dataScope);
            }
        }

        return documentRetrievers;
    }

}
