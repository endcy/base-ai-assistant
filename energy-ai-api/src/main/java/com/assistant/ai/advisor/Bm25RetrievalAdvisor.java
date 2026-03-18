package com.assistant.ai.advisor;

import cn.hutool.core.util.StrUtil;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.constant.EnergyAiConstant;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.repository.domain.vector.VectorDocument;
import com.assistant.ai.repository.service.VectorStoreService;
import com.assistant.ai.util.DocumentConvertUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.generation.augmentation.QueryAugmenter;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.scheduler.Scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BM25 关键词检索增强顾问
 * 支持元数据过滤条件
 *
 * @author endcy
 * @date 2025/12/2 17:54:57
 */
@Slf4j
public class Bm25RetrievalAdvisor implements BaseAdvisor {

    public static final String DOCUMENT_CONTEXT = "rag_document_context";

    private final VectorStoreService vectorStoreService;

    private final List<DocumentPostProcessor> documentPostProcessors;

    private final QueryAugmenter queryAugmenter;

    private final ChatRagProperties chatRagProperties;

    private final DocumentQueryContext documentParams;

    private Bm25RetrievalAdvisor(@Nullable List<QueryTransformer> queryTransformers,
                                 @Nullable List<DocumentPostProcessor> documentPostProcessors,
                                 @Nullable QueryAugmenter queryAugmenter,
                                 @Nullable VectorStoreService vectorStoreService,
                                 @Nullable DocumentQueryContext documentParams,
                                 @Nullable ChatRagProperties chatRagProperties) {
        this.documentPostProcessors = documentPostProcessors != null ? documentPostProcessors : List.of();
        this.queryAugmenter = queryAugmenter != null ? queryAugmenter : ContextualQueryAugmenter.builder().build();
        this.vectorStoreService = vectorStoreService;
        this.chatRagProperties = chatRagProperties;
        this.documentParams = documentParams;
    }

    public static Bm25RetrievalAdvisor.Builder builder() {
        return new Bm25RetrievalAdvisor.Builder();
    }

    @NotNull
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, @Nullable AdvisorChain advisorChain) {
        Map<String, Object> context = new HashMap<>(chatClientRequest.context());
        Query originalQuery = Query.builder()
                                   .text(chatClientRequest.prompt().getUserMessage().getText())
                                   .context(context)
                                   .build();
        String question = (String) originalQuery.context().get("re2_input_query");
        if (StrUtil.isBlank(question) || StrUtil.isBlank(documentParams.getOriginalQuestion())) {
            return chatClientRequest;
        }

        // bm25 检索
        List<VectorDocument> bm25Documents = vectorStoreService.retrieveWithTsQuery(documentParams, chatRagProperties.getBm25TopK(), chatRagProperties.getBm25SimilarityThreshold());
        List<Document> documents = DocumentConvertUtils.vectorConvertDocument(bm25Documents);

        for (var documentPostProcessor : this.documentPostProcessors) {
            documents = documentPostProcessor.process(originalQuery, documents);
        }
        context.put(DOCUMENT_CONTEXT, documents);

        Query augmentedQuery = this.queryAugmenter.augment(originalQuery, documents);

        return chatClientRequest.mutate()
                                .prompt(chatClientRequest.prompt().augmentUserMessage(augmentedQuery.text()))
                                .context(context)
                                .build();
    }

    @NotNull
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, @Nullable AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder;
        if (chatClientResponse.chatResponse() == null) {
            chatResponseBuilder = ChatResponse.builder();
        } else {
            chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        }
        chatResponseBuilder.metadata(DOCUMENT_CONTEXT, chatClientResponse.context().get(DOCUMENT_CONTEXT));
        return ChatClientResponse.builder()
                                 .chatResponse(chatResponseBuilder.build())
                                 .context(chatClientResponse.context())
                                 .build();
    }

    @NotNull
    @Override
    public Scheduler getScheduler() {
        return BaseAdvisor.DEFAULT_SCHEDULER;
    }

    @Override
    public int getOrder() {
        return EnergyAiConstant.BM25_ADVISOR_ORDER;
    }


    public static final class Builder {

        private List<QueryTransformer> queryTransformers;

        private List<DocumentPostProcessor> documentPostProcessors;

        private QueryAugmenter queryAugmenter;

        private VectorStoreService vectorStoreService;

        private ChatRagProperties chatRagProperties;

        private DocumentQueryContext documentParams;

        private Builder() {
        }

        public Bm25RetrievalAdvisor.Builder vectorStoreService(VectorStoreService vectorStoreService) {
            Assert.notNull(vectorStoreService, "vectorStoreService cannot be null");
            this.vectorStoreService = vectorStoreService;
            return this;
        }

        public Bm25RetrievalAdvisor.Builder queryTransformers(List<QueryTransformer> queryTransformers) {
            this.queryTransformers = queryTransformers;
            return this;
        }

        public Bm25RetrievalAdvisor.Builder chatRagProperties(ChatRagProperties chatRagProperties) {
            this.chatRagProperties = chatRagProperties;
            return this;
        }

        public Bm25RetrievalAdvisor.Builder documentPostProcessors(List<DocumentPostProcessor> documentPostProcessors) {
            this.documentPostProcessors = documentPostProcessors;
            return this;
        }

        public Bm25RetrievalAdvisor.Builder queryAugmenter(QueryAugmenter queryAugmenter) {
            this.queryAugmenter = queryAugmenter;
            return this;
        }

        public Bm25RetrievalAdvisor.Builder documentParams(DocumentQueryContext documentParams) {
            this.documentParams = documentParams;
            return this;
        }

        public Bm25RetrievalAdvisor build() {
            return new Bm25RetrievalAdvisor(this.queryTransformers, this.documentPostProcessors,
                    this.queryAugmenter, this.vectorStoreService, this.documentParams,
                    this.chatRagProperties);
        }

    }

}
