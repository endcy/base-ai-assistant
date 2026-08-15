package com.endcy.ai.app;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.endcy.ai.advisor.ChatClientAdvisorFactory;
import com.endcy.ai.advisor.PromptLoggerAdvisor;
import com.endcy.ai.agent.IntentAnalysisAgent;
import com.endcy.ai.agent.MediaAnalysisAgent;
import com.endcy.ai.agent.model.IntentResult;
import com.endcy.ai.config.ChatRagProperties;
import com.endcy.ai.domain.context.RequestRagContext;
import com.endcy.ai.manager.ChatHistoryService;
import com.endcy.ai.mcp.config.McpConfig;
import com.endcy.ai.rag.QueryRewriter;
import com.endcy.ai.repository.domain.context.DocumentQueryContext;
import com.endcy.ai.repository.domain.dto.ContextUserRecordDTO;
import com.endcy.ai.repository.service.ContextUserRecordService;
import com.endcy.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.endcy.ai.rpc.domain.request.MediaAttachment;
import com.endcy.ai.util.UserChatPromptUtils;
import com.endcy.service.domain.enums.KnowledgeBusinessTypeEnum;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 能源 AI 应用主服务类。
 *
 * @author endcy
 * @date 2025/10/31
 */
@Component
@Slf4j
@RequiredArgsConstructor
@DependsOn("vectorStoreManager")
@Import(McpConfig.class)
public class EnergyAiApp {

    private final ChatClient commonChatClient;

    private final QueryRewriter queryRewriter;

    private final ChatClientAdvisorFactory chatClientAdvisorFactory;

    private final IntentAnalysisAgent intentAnalysisAgent;

    private final ChatRagProperties chatRagProperties;

    private final ContextUserRecordService userRecordService;

    private final SyncMcpToolCallbackProvider mcpToolCallbacks;

    private final MediaAnalysisAgent mediaAnalysisAgent;

    private final ToolCallback[] ragTools;

    private final ChatHistoryService chatHistoryService;

    private final ChatClient simpleChatClient;

    /**
     * Parse externally provided String chatId to Long; non-numeric returns null.
     */
    private static Long parseChatId(String chatId) {
        if (StrUtil.isBlank(chatId)) {
            return null;
        }
        try {
            return Long.parseLong(chatId);
        } catch (NumberFormatException e) {
            return null;
        }
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
     * Simple AI Q&amp;A (supports multimodal + multi-turn conversation memory).
     */
    public String simpleChat(KnowledgeAIQueryParam query, RequestRagContext requestRagContext) {
        List<Message> existingMessages = chatHistoryService.loadHistoryFromDb(query.getChatId());

        String mediaAnalysisText = mediaAnalysisAgent.analyze(
                query.getChatId(), query.getQuestion(), query.getMediaList());

        ContextUserRecordDTO userRecord = ContextUserRecordDTO.builder()
                                                              .chatId(query.getChatId())
                                                              .groupId(query.getGroupId())
                                                              .scopeType(query.getScopeType())
                                                              .question(query.getQuestion())
                                                              .mediaInfo(buildMediaInfoJson(query))
                                                              .build();
        userRecordService.insert(userRecord);

        PromptLoggerAdvisor promptLogger = chatClientAdvisorFactory.createPromptLoggerAdvisor(requestRagContext);
        List<Advisor> dataResourceAdvisors = CollUtil.newArrayList(promptLogger);

        long t1 = System.currentTimeMillis();
        ChatResponse chatResponse = simpleChatClient
                .prompt()
                .user(UserChatPromptUtils.generatePromptUserSpecConsumer(query, mediaAnalysisText))
                .messages(existingMessages)
                .advisors(dataResourceAdvisors)
                .call()
                .chatResponse();
        log.info("simpleChatClient call simpleChat cost: {} ms", System.currentTimeMillis() - t1);

        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
            userRecordService.updateAnswerById(userRecord.getId(), content);
        }
        if (log.isDebugEnabled()) {
            log.debug("content: {}", content);
        }
        return content;
    }

    /**
     * AI basic chat (DB-persisted multi-turn conversation memory).
     */
    public String doChat(String message, String chatId) {
        Long chatIdLong = parseChatId(chatId);
        List<Message> history = chatIdLong != null
                ? chatHistoryService.loadHistoryFromDb(chatIdLong)
                : Collections.emptyList();

        ContextUserRecordDTO record = null;
        if (chatIdLong != null) {
            record = ContextUserRecordDTO.builder().chatId(chatIdLong).question(message).build();
            userRecordService.insert(record);
        }

        long t1 = System.currentTimeMillis();
        ChatResponse chatResponse = commonChatClient.prompt()
                                                    .user(message)
                                                    .messages(history)
                                                    .call()
                                                    .chatResponse();
        log.info("commonChatClient call doChat cost: {} ms", System.currentTimeMillis() - t1);

        if (chatResponse == null) {
            return "请求异常";
        }
        String content = chatResponse.getResult().getOutput().getText();
        if (record != null && StrUtil.isNotBlank(content)) {
            userRecordService.updateAnswerById(record.getId(), content);
        }
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI basic chat SSE streaming (DB-persisted multi-turn conversation memory).
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        Long chatIdLong = parseChatId(chatId);
        List<Message> history = chatIdLong != null
                ? chatHistoryService.loadHistoryFromDb(chatIdLong)
                : Collections.emptyList();
        final ContextUserRecordDTO record = chatIdLong != null
                ? insertQuestionRecord(chatIdLong, message)
                : null;

        final StringBuilder answerBuffer = new StringBuilder();
        return commonChatClient.prompt()
                               .user(message)
                               .messages(history)
                               .stream()
                               .content()
                               .doOnNext(answerBuffer::append)
                               .doOnComplete(() -> {
                                   if (record != null) {
                                       String answer = answerBuffer.toString();
                                       if (StrUtil.isNotBlank(answer)) {
                                           userRecordService.updateAnswerById(record.getId(), answer);
                                       }
                                   }
                               });
    }

    /**
     * Chat with RAG knowledge base.
     *
     * @param mediaList multimedia attachment list, can be null
     */
    public String doChatWithRag(String scopeType, String groupId, String message, @NonNull Long chatId, List<MediaAttachment> mediaList) {
        long t1 = System.currentTimeMillis();
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        log.info("queryRewriter doQueryRewrite cost: {} ms", System.currentTimeMillis() - t1);

        String mediaAnalysisText = mediaAnalysisAgent.analyze(chatId, message, mediaList);

        DocumentQueryContext documentParams = new DocumentQueryContext();
        documentParams.setOriginalQuestion(message);
        if (StrUtil.isNotBlank(mediaAnalysisText)) {
            documentParams.setReReadingQuestion(rewrittenMessage + "\n" + mediaAnalysisText);
        } else {
            documentParams.setReReadingQuestion(rewrittenMessage);
        }
        scopeType = StrUtil.blankToDefault(scopeType, "用户客服");
        documentParams.setScopeType(scopeType);
        if (groupId != null) {
            documentParams.setGroupId(groupId);
        }

        long t3 = System.currentTimeMillis();
        IntentResult intentResult = intentAnalysisAgent.analyzeIntent(chatId, scopeType, rewrittenMessage);
        log.info("intentAnalysisAgent analyzeQuestion cost: {} ms", System.currentTimeMillis() - t3);
        if (BooleanUtil.isTrue(chatRagProperties.getEnableIntentAnalysis())) {
            if (!KnowledgeBusinessTypeEnum.UNKNOWN.getType().equals(intentResult.getBusinessType())) {
                documentParams.setBusinessType(intentResult.getBusinessType());
            }
        }

        ContextUserRecordDTO userRecord = ContextUserRecordDTO.builder()
                                                              .chatId(chatId)
                                                              .groupId(groupId)
                                                              .scopeType(scopeType)
                                                              .businessType(documentParams.getBusinessType())
                                                              .question(message)
                                                              .mediaInfo(CollUtil.isNotEmpty(mediaList) ? JSONUtil.toJsonStr(mediaList) : null)
                                                              .build();
        userRecordService.insert(userRecord);

        List<Message> existingMessages = chatHistoryService.loadHistoryFromDb(chatId);
        log.info("###### Chat memory for {}: {} messages size", chatId, existingMessages.size());

        List<Advisor> dataResourceAdvisors = CollUtil.newArrayList();
        RequestRagContext requestRagContext = new RequestRagContext();
        requestRagContext.setChatId(chatId);
        dataResourceAdvisors.add(chatClientAdvisorFactory.createHybridRetrievalAdvisor(documentParams, intentResult, requestRagContext));

        Consumer<ChatClient.PromptUserSpec> userSpecConsumer = UserChatPromptUtils.generatePromptUserSpecConsumer(
                buildKnowledgeParam(message, chatId, mediaList), mediaAnalysisText);

        long t4 = System.currentTimeMillis();
        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(userSpecConsumer)
                .messages(existingMessages)
                .toolCallbacks(mcpToolCallbacks.getToolCallbacks())
                .toolCallbacks(ragTools)
                .advisors(dataResourceAdvisors)
                .call()
                .chatResponse();
        log.info("commonChatClient call doChatWithRag cost: {} ms", System.currentTimeMillis() - t4);
        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
            userRecordService.updateAnswerById(userRecord.getId(), content);
        }
        if (log.isDebugEnabled()) {
            log.debug("content: {}", content);
        }
        return content;
    }

    private KnowledgeAIQueryParam buildKnowledgeParam(String message, Long chatId, List<MediaAttachment> mediaList) {
        KnowledgeAIQueryParam param = new KnowledgeAIQueryParam();
        param.setChatId(chatId);
        param.setQuestion(message);
        param.setMediaList(mediaList);
        return param;
    }

    private ContextUserRecordDTO insertQuestionRecord(Long chatId, String question) {
        ContextUserRecordDTO record = ContextUserRecordDTO.builder()
                                                          .chatId(chatId)
                                                          .question(question)
                                                          .build();
        userRecordService.insert(record);
        return record;
    }
}
