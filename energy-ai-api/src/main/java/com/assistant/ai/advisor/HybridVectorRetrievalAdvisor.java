package com.assistant.ai.advisor;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
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
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;

/**
 * 统一数据库来源文档检索
 * 包含了 bm25 关键词检索 + 语义向量相似检索 + 重排序的 Advisor
 *
 * @author endcy
 * @date 2025/12/3 20:11:46
 */
@Slf4j
public class HybridVectorRetrievalAdvisor implements BaseAdvisor {

    private final VectorStore vectorStore;

    private final RerankModel rerankModel;

    private final SearchRequest searchRequest;

    private final VectorStoreService vectorStoreService;

    private final ChatRagProperties chatRagProperties;

    private final DocumentQueryContext documentParams;

    public static final String RETRIEVED_DOCUMENTS = "qa_retrieved_documents";

    public static final String FILTER_EXPRESSION = "qa_filter_expression";


    public HybridVectorRetrievalAdvisor(VectorStore vectorStore,
                                        RerankModel rerankModel,
                                        SearchRequest searchRequest,
                                        VectorStoreService vectorStoreService,
                                        DocumentQueryContext documentParams,
                                        ChatRagProperties chatRagProperties) {
        Assert.notNull(vectorStore, "The vectorStore must not be null!");
        Assert.notNull(rerankModel, "The rerankModel must not be null!");
        Assert.notNull(searchRequest, "The searchRequest must not be null!");
        Assert.notNull(vectorStoreService, "The vectorStoreService must not be null!");
        Assert.notNull(chatRagProperties, "The chatRagProperties must not be null!");
        Assert.notNull(documentParams, "The documentParams must not be null!");
        this.vectorStore = vectorStore;
        this.rerankModel = rerankModel;
        this.searchRequest = searchRequest;
        this.vectorStoreService = vectorStoreService;
        this.chatRagProperties = chatRagProperties;
        this.documentParams = documentParams;
    }

    @Override
    public int getOrder() {
        return EnergyAiConstant.HYBRID_VECTOR_ADVISOR_ORDER;
    }

    protected List<Document> doRerank(ChatClientRequest request, List<Document> documents) {
        if (CollUtil.isEmpty(documents)) {
            return documents;
        }

        var rerankRequest = new RerankRequest(request.prompt().getUserMessage().getText(), documents);

        RerankResponse response = rerankModel.call(rerankRequest);
        log.debug("reranked documents: {}", response);
        if (CollUtil.isEmpty(response.getResults())) {
            return documents;
        }

        return response.getResults()
                       .stream()
                       .filter(doc -> doc != null && doc.getScore() >= chatRagProperties.getRerankMinScore())
                       .sorted(java.util.Comparator.comparingDouble(DocumentWithScore::getScore).reversed())
                       .map(DocumentWithScore::getOutput)
                       .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 文档合并
     * 生成的 Document id 不能为空
     *
     * @param toAddDocuments .
     * @param documents      .
     * @return .
     */
    private static List<Document> mergeDocuments(List<Document> toAddDocuments, List<Document> documents) {
        List<Document> mergedDocuments = CollUtil.newArrayList(documents);
        if (CollUtil.isNotEmpty(toAddDocuments)) {
            java.util.Set<String> documentIds = mergedDocuments.stream().map(Document::getId).collect(java.util.stream.Collectors.toSet());
            toAddDocuments.forEach(doc -> {
                if (!documentIds.contains(doc.getId())) {
                    mergedDocuments.add(doc);
                }
            });
        }
        return mergedDocuments;
    }

    @NotNull
    @Override
    public ChatClientRequest before(ChatClientRequest request, @NotNull AdvisorChain advisorChain) {
        var context = request.context();
        var userMessage = request.prompt().getUserMessage();
        var searchRequestToUse = SearchRequest.from(this.searchRequest)
                                              .query(userMessage.getText())
                                              .build();

        // 向量相似度检索
        List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);
        context.put(RETRIEVED_DOCUMENTS, documents);

        // bm25 检索
        List<VectorDocument> bm25DocumentList = null;
        if (chatRagProperties.getEnableBm25Query()) {
            bm25DocumentList = vectorStoreService.retrieveWithTsQuery(documentParams, chatRagProperties.getBm25TopK(), chatRagProperties.getBm25SimilarityThreshold());
        }
        List<Document> bm25Documents = DocumentConvertUtils.vectorConvertDocument(bm25DocumentList);

        //文档合并
        List<Document> mergedDocuments = mergeDocuments(documents, bm25Documents);

        // 重排序
        List<Document> rerankedDocuments = doRerank(request, mergedDocuments);

        String documentContext = rerankedDocuments.stream()
                                                  .map(Document::getText)
                                                  .collect(java.util.stream.Collectors.joining(System.lineSeparator()));

        String augmentedUserText = EnergyAiConstant.DEFAULT_PROMPT_TEMPLATE.render(Map.of("query", userMessage.getText(), "question_answer_context", documentContext));

        // Update ChatClientRequest with augmented prompt.
        return request.mutate().prompt(request.prompt().augmentUserMessage(augmentedUserText)).context(context).build();
    }

    @NotNull
    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, @NotNull AdvisorChain advisorChain) {
        ChatResponse.Builder chatResponseBuilder;
        if (chatClientResponse.chatResponse() == null) {
            chatResponseBuilder = ChatResponse.builder();
        } else {
            chatResponseBuilder = ChatResponse.builder().from(chatClientResponse.chatResponse());
        }
        chatResponseBuilder.metadata(RETRIEVED_DOCUMENTS, chatClientResponse.context().get(RETRIEVED_DOCUMENTS));
        return ChatClientResponse.builder()
                                 .chatResponse(chatResponseBuilder.build())
                                 .context(chatClientResponse.context())
                                 .build();
    }

}
