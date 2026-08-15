package com.endcy.ai.app;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.endcy.ai.advisor.ChatClientAdvisorFactory;
import com.endcy.ai.agent.MediaAnalysisAgent;
import com.endcy.ai.agent.model.IntentResult;
import com.endcy.ai.domain.context.ChatConfigResult;
import com.endcy.ai.domain.context.RequestRagContext;
import com.endcy.ai.manager.ChatHistoryService;
import com.endcy.ai.mcp.config.McpConfig;
import com.endcy.ai.rag.QueryRewriter;
import com.endcy.ai.repository.domain.context.DocumentQueryContext;
import com.endcy.ai.repository.domain.dto.ContextUserRecordDTO;
import com.endcy.ai.repository.service.ContextUserRecordService;
import com.endcy.ai.rpc.domain.base.AIStreamResponse;
import com.endcy.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.endcy.ai.rpc.domain.request.MediaAttachment;
import com.endcy.ai.rpc.domain.response.KnowledgeDocumentMatchItem;
import com.endcy.ai.rpc.enums.ApiQaType;
import com.endcy.ai.rpc.enums.MessageType;
import com.endcy.ai.tools.DeepSeekWebSearchTool;
import com.endcy.ai.util.DocumentConvertUtils;
import com.endcy.ai.util.UserChatPromptUtils;
import com.endcy.service.common.exception.CoException;
import com.endcy.service.domain.enums.PossibleSourceTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 能源 AI 文档应用服务类。
 * 提供 RAG 知识库问答、DeepSeek 搜索以及流式对话功能。
 *
 * @author endcy
 * @date 2025/10/31
 */
@Component
@Slf4j
@RequiredArgsConstructor
@DependsOn("vectorStoreManager")
@Import(McpConfig.class)
public class EnergyAiDocumentApp {

    private final ChatClient commonChatClient;

    private final QueryRewriter queryRewriter;

    private final ChatClientAdvisorFactory chatClientAdvisorFactory;

    private final ContextUserRecordService userRecordService;

    private final DeepSeekWebSearchTool deepSeekWebSearchTool;

    private final ChatHistoryService chatHistoryService;

    private final MediaAnalysisAgent mediaAnalysisAgent;

    /**
     * Build DocumentQueryContext from query, rewritten text, and optional media analysis.
     */
    @NotNull
    private static DocumentQueryContext buildDocumentParams(KnowledgeAIQueryParam query,
                                                            String rewrittenMessage,
                                                            String mediaAnalysisText) {
        DocumentQueryContext params = new DocumentQueryContext();
        params.setOriginalQuestion(query.getQuestion());
        if (StrUtil.isNotBlank(mediaAnalysisText)) {
            params.setReReadingQuestion(rewrittenMessage + "\n" + mediaAnalysisText);
        } else {
            params.setReReadingQuestion(rewrittenMessage);
        }
        params.setScopeType(StrUtil.blankToDefault(query.getScopeType(), "用户客服"));
        if (toLongGroupId(query.getGroupId()) != null) {
            params.setGroupId(toLongGroupId(query.getGroupId()));
        }
        return params;
    }

    /**
     * Build IntentResult — hardcoded VECTOR scope.
     */
    @NotNull
    private static IntentResult buildIntentResult(KnowledgeAIQueryParam query, String rewrittenMessage) {
        IntentResult intent = new IntentResult();
        intent.setChatId(query.getChatId());
        intent.setScopeType(query.getScopeType());
        intent.setUserMessage(rewrittenMessage);
        intent.setDataScopeList(CollUtil.newArrayList(PossibleSourceTypeEnum.VECTOR));
        return intent;
    }

    /**
     * Serialize multimedia attachment list to JSON string for DB storage.
     */
    private static String buildMediaInfoJson(KnowledgeAIQueryParam query) {
        List<MediaAttachment> mediaList = query.getMediaList();
        if (CollUtil.isEmpty(mediaList)) {
            return null;
        }
        return JSONUtil.toJsonStr(mediaList);
    }

    /**
     * AI RAG 知识库对话。
     */
    public String doChatRag(KnowledgeAIQueryParam query, RequestRagContext requestRagContext) {
        ChatConfigResult chatConfig = chatClientConfig(query, requestRagContext);
        long t1 = System.currentTimeMillis();
        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(UserChatPromptUtils.generatePromptUserSpecConsumer(query, chatConfig.getMediaAnalysisText()))
                .messages(chatConfig.getExistingMessages())
                .advisors(chatConfig.getDataResourceAdvisors())
                .call()
                .chatResponse();
        log.info("commonChatClient call doChatRag cost: {} ms", System.currentTimeMillis() - t1);
        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
            userRecordService.updateAnswerById(chatConfig.getUserRecord().getId(), content);
        }
        if (log.isDebugEnabled()) {
            log.debug("content: {}", content);
        }
        return content;
    }

    /**
     * DeepSeek 搜索。
     */
    public String deepseek(KnowledgeAIQueryParam query) {
        ContextUserRecordDTO userRecord = ContextUserRecordDTO.builder()
                                                              .chatId(query.getChatId())
                                                              .groupId(toLongGroupId(query.getGroupId()))
                                                              .scopeType(query.getScopeType())
                                                              .businessType(query.getBusinessType())
                                                              .question(query.getQuestion())
                                                              .build();
        userRecordService.insert(userRecord);
        String content;
        long t1 = System.currentTimeMillis();
        try {
            content = deepSeekWebSearchTool.searchQuestion(null, query.getQuestion());
        } catch (Exception e) {
            log.error("deepseek search error", e);
            content = "deepseek search error " + e.getMessage();
        }
        log.info("deepseek search cost: {} ms", System.currentTimeMillis() - t1);
        userRecordService.updateAnswerById(userRecord.getId(), content);
        if (log.isDebugEnabled()) {
            log.debug("content: {}", content);
        }
        return content;
    }

    /**
     * RAG pipeline configuration — assembles query params, intent, history, and advisors.
     */
    @NotNull
    private ChatConfigResult chatClientConfig(KnowledgeAIQueryParam query, RequestRagContext requestRagContext) {
        String rewrittenMessage = rewriteQuery(query);
        String mediaAnalysisText = analyzeMedia(query);

        DocumentQueryContext documentParams = buildDocumentParams(query, rewrittenMessage, mediaAnalysisText);
        IntentResult intentResult = buildIntentResult(query, rewrittenMessage);
        ContextUserRecordDTO userRecord = persistUserRecord(query, documentParams);

        List<Message> existingMessages = loadChatHistory(query);
        List<Advisor> advisors = assembleAdvisors(query, documentParams, intentResult, requestRagContext);

        ChatConfigResult result = new ChatConfigResult();
        result.setRewrittenMessage(rewrittenMessage);
        result.setUserRecord(userRecord);
        result.setExistingMessages(existingMessages);
        result.setDataResourceAdvisors(advisors);
        result.setMediaAnalysisText(mediaAnalysisText);
        return result;
    }

    private String rewriteQuery(KnowledgeAIQueryParam query) {
        return queryRewriter.doQueryRewrite(query.getQuestion());
    }

    private String analyzeMedia(KnowledgeAIQueryParam query) {
        return mediaAnalysisAgent.analyze(query.getChatId(), query.getQuestion(), query.getMediaList());
    }

    private ContextUserRecordDTO persistUserRecord(KnowledgeAIQueryParam query, DocumentQueryContext docParams) {
        ContextUserRecordDTO record = ContextUserRecordDTO.builder()
                                                          .chatId(query.getChatId())
                                                          .groupId(toLongGroupId(query.getGroupId()))
                                                          .scopeType(query.getScopeType())
                                                          .businessType(docParams.getBusinessType())
                                                          .question(query.getQuestion())
                                                          .mediaInfo(buildMediaInfoJson(query))
                                                          .build();
        userRecordService.insert(record);
        return record;
    }

    private List<Message> loadChatHistory(KnowledgeAIQueryParam query) {
        List<Message> messages = chatHistoryService.loadHistoryFromDb(query.getChatId());
        log.info("###### Chat memory for {}: {} messages size", query.getChatId(), messages.size());
        return messages;
    }

    private List<Advisor> assembleAdvisors(KnowledgeAIQueryParam query,
                                           DocumentQueryContext docParams,
                                           IntentResult intent,
                                           RequestRagContext ctx) {
        List<Advisor> advisors = CollUtil.newArrayList(
                chatClientAdvisorFactory.createPromptLoggerAdvisor(ctx));
        ApiQaType qaType = ApiQaType.getByCode(query.getQueryType());
        if (qaType == ApiQaType.RAG) {
            advisors.add(chatClientAdvisorFactory.createHybridRetrievalAdvisor(docParams, intent, ctx));
        }
        return advisors;
    }

    /**
     * AI RAG 知识库 SSE 流式对话。
     */
    public Flux<AIStreamResponse> doChatRagStream(KnowledgeAIQueryParam query, RequestRagContext requestRagContext) {
        ChatConfigResult chatConfig = chatClientConfig(query, requestRagContext);

        final StringBuilder answerBuffer = new StringBuilder();

        Flux<AIStreamResponse> textStream = commonChatClient
                .prompt()
                .user(UserChatPromptUtils.generatePromptUserSpecConsumer(query, chatConfig.getMediaAnalysisText()))
                .messages(chatConfig.getExistingMessages())
                .advisors(chatConfig.getDataResourceAdvisors())
                .stream()
                .chatResponse()
                .map(chatResponse -> {
                    String text = chatResponse != null
                            ? chatResponse.getResult().getOutput().getText() : "";
                    if (text != null) {
                        answerBuffer.append(text);
                    }
                    return createTextChunkResponse(query.getChatId(), text);
                })
                .doOnNext(resp -> resp.setFinal(false))
                .doOnComplete(() -> {
                    String answer = answerBuffer.toString();
                    if (StrUtil.isNotBlank(answer)) {
                        userRecordService.updateAnswerById(chatConfig.getUserRecord().getId(), answer);
                    }
                })
                .onErrorMap(throwable -> new CoException("AI服务调用失败", throwable));

        Mono<AIStreamResponse> documentMono = Mono.fromCallable(() ->
                createDocumentChunkResponse(query.getChatId(), requestRagContext));

        Mono<AIStreamResponse> tokenMono = Mono.fromCallable(() ->
                createTokenChunkResponse(query.getChatId(), requestRagContext));

        return textStream
                .concatWith(documentMono)
                .concatWith(tokenMono)
                .onErrorMap(throwable -> new CoException("AI服务调用失败", throwable));
    }

    private AIStreamResponse createTextChunkResponse(Long chatId, String chunk) {
        AIStreamResponse response = new AIStreamResponse();
        response.setType(MessageType.TEXT);
        response.setData(chunk);
        response.setChatId(chatId);
        response.setFinal(false);
        return response;
    }

    private AIStreamResponse createDocumentChunkResponse(Long chatId, RequestRagContext requestRagContext) {
        List<KnowledgeDocumentMatchItem> relatedDocs = DocumentConvertUtils.documentConvertRelated(requestRagContext.getRelatedDocuments());
        AIStreamResponse response = new AIStreamResponse();
        response.setType(MessageType.DOC);
        response.setData(JSONUtil.toJsonStr(relatedDocs));
        response.setChatId(chatId);
        response.setFinal(false);
        return response;
    }

    private AIStreamResponse createTokenChunkResponse(Long chatId, RequestRagContext requestRagContext) {
        AIStreamResponse response = new AIStreamResponse();
        response.setType(MessageType.TOKEN);
        int promptTokens = requestRagContext != null ? requestRagContext.getPromptTokens() : 0;
        int completionTokens = requestRagContext != null ? requestRagContext.getCompletionTokens() : 0;
        response.setData(JSONUtil.toJsonStr(CollUtil.newArrayList(promptTokens, completionTokens)));
        response.setChatId(chatId);
        response.setFinal(true);
        return response;
    }

    /**
     * Convert String groupId (RPC) to Long (DB); "-1" or non-numeric returns null.
     */
    private static Long toLongGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(groupId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
