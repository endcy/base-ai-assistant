package com.assistant.ai.advisor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.document.DocumentWithScore;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.model.RerankRequest;
import com.alibaba.cloud.ai.model.RerankResponse;
import com.assistant.ai.advisor.retriever.BaseDocumentRetriever;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.constant.EnergyAiConstant;
import com.assistant.ai.domain.context.RequestRagContext;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 自定义文档来源检索器的检索增强器
 * 包含了 本地向量化文档、pg 库表文档 bm25 关键词检索 + pg 库表语义向量相似检索 + 重排序的 Advisor
 * 原 CompositeDocumentRetriever 实现多路检索 需要额外重排序 且串行检索效率较低
 *
 * @author endcy
 * @date 2025/12/3 20:11:46
 */
@Slf4j
public class HybridRetrievalAdvisor implements BaseAdvisor {

    private final RerankModel rerankModel;

    private final ChatRagProperties chatRagProperties;

    private final List<BaseDocumentRetriever> documentRetrievers;

    private final QueryExpander queryExpander;

    private final RequestRagContext requestRagContext;


    public static final String RETRIEVED_DOCUMENTS = "rag_retrieved_documents";

    public HybridRetrievalAdvisor(RerankModel rerankModel,
                                  ChatRagProperties chatRagProperties,
                                  List<BaseDocumentRetriever> documentRetrievers,
                                  QueryExpander queryExpander,
                                  RequestRagContext requestRagContext) {
        Assert.notNull(rerankModel, "The rerankModel must not be null!");
        Assert.notNull(chatRagProperties, "The chatRagProperties must not be null!");
        this.documentRetrievers = documentRetrievers;
        this.queryExpander = queryExpander;
        this.rerankModel = rerankModel;
        this.chatRagProperties = chatRagProperties;
        this.requestRagContext = requestRagContext;
    }

    @Override
    public int getOrder() {
        return EnergyAiConstant.HYBRID_ADVISOR_ORDER;
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
                       .sorted(Comparator.comparingDouble(DocumentWithScore::getScore).reversed())
                       .map(DocumentWithScore::getOutput)
                       .collect(Collectors.toList());
    }

    /**
     * 文档合并
     * 生成的 Document id 不能为空
     *
     * @param documentsList .
     * @return .
     */
    private static List<Document> mergeDocuments(List<List<Document>> documentsList) {
        if (CollUtil.isEmpty(documentsList)) {
            return CollUtil.newArrayList();
        }
        // 将 documentsList 合并为一个文档 List 并去重
        return new ArrayList<>(documentsList.stream()
                                            .flatMap(List::stream)
                                            .collect(Collectors.toMap(
                                                    Document::getId,
                                                    Function.identity(),
                                                    (existing, replacement) -> existing
                                            ))
                                            .values());
    }

    private static TaskExecutor buildDefaultTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setThreadNamePrefix("ai-advisor-");
        taskExecutor.setCorePoolSize(4);
        taskExecutor.setMaxPoolSize(8);
        taskExecutor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        taskExecutor.initialize();
        return taskExecutor;
    }

    @NotNull
    @Override
    public ChatClientRequest before(ChatClientRequest request, @NotNull AdvisorChain advisorChain) {
        var context = request.context();
        var userMessage = request.prompt().getUserMessage();
        Query originalQuery = Query.builder()
                                   .text(request.prompt().getUserMessage().getText())
                                   .history(request.prompt().getInstructions())
                                   .context(context)
                                   .build();
        var userQuery = request.prompt().getUserMessage().getText();
        var enableSplits = StrUtil.length(userQuery) > chatRagProperties.getQuerySplitsWordNum() && queryExpander != null;
        List<Query> querySplits = enableSplits ? queryExpander.expand(originalQuery) : List.of(originalQuery);

        String augmentedUserText;

        if (CollUtil.isNotEmpty(documentRetrievers)) {
            // 按检索器分割任务
            List<List<Document>> documentsList = documentRetrievers.stream()
                                                                   .map(retriever -> CompletableFuture.supplyAsync(() -> retriever.retrieve(querySplits), buildDefaultTaskExecutor()))
                                                                   .toList()
                                                                   .stream()
                                                                   .map(CompletableFuture::join)
                                                                   .toList();
            //文档合并
            List<Document> mergedDocuments = mergeDocuments(documentsList);

            context.put(RETRIEVED_DOCUMENTS, mergedDocuments);
            // 重排序
            List<Document> rerankedDocuments = doRerank(request, mergedDocuments);
            //加入上下文
            if (requestRagContext != null) {
                requestRagContext.setRelatedDocuments(rerankedDocuments);
            }

            String documentContext = rerankedDocuments.stream()
                                                      .map(Document::getText)
                                                      .collect(Collectors.joining(System.lineSeparator()));
            augmentedUserText = EnergyAiConstant.DEFAULT_PROMPT_TEMPLATE.render(Map.of("query", userMessage.getText(), "question_answer_context", documentContext));
        } else {
            augmentedUserText = EnergyAiConstant.EMPTY_PROMPT_TEMPLATE.render(Map.of("query", userMessage.getText()));
        }

        // 增强提示词
        return request.mutate()
                      .prompt(request.prompt().augmentUserMessage(augmentedUserText))
                      .context(context)
                      .build();
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
