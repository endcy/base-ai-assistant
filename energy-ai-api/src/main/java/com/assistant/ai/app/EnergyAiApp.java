package com.assistant.ai.app;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.assistant.ai.advisor.ChatClientAdvisorFactory;
import com.assistant.ai.advisor.HybridRetrievalAdvisor;
import com.assistant.ai.advisor.PromptLoggerAdvisor;
import com.assistant.ai.agent.IntentAnalysisAgent;
import com.assistant.ai.agent.model.IntentResult;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.domain.context.ChatConfigResult;
import com.assistant.ai.domain.context.RequestRagContext;
import com.assistant.ai.manager.ChatHistoryService;
import com.assistant.ai.mcp.config.McpConfig;
import com.assistant.ai.rag.QueryRewriter;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.repository.domain.dto.ContextUserRecordDTO;
import com.assistant.ai.repository.service.ContextUserRecordService;
import com.assistant.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.assistant.ai.rpc.domain.request.MediaAttachment;
import com.assistant.ai.util.UserChatPromptUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

/**
 * 能源 AI 应用主服务类
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
    //对话提示词重写器
    private final QueryRewriter queryRewriter;
    //通用顾问工厂
    private final ChatClientAdvisorFactory chatClientAdvisorFactory;
    //意图分析
    private final IntentAnalysisAgent intentAnalysisAgent;

    private final ChatRagProperties chatRagProperties;

    private final ContextUserRecordService userRecordService;

    private final SyncMcpToolCallbackProvider mcpToolCallbacks;

    private final MessageWindowChatMemory messageWindowChatMemory;

    private final PgVectorStore pgVectorVectorStore;

    private final ToolCallback[] ragTools;

    private final ChatHistoryService chatHistoryService;


    /**
     * AI 简单进行问答（支持多模态）
     */
    public String simpleChat(KnowledgeAIQueryParam query, RequestRagContext requestRagContext) {
        DocumentQueryContext documentParams = new DocumentQueryContext();
        documentParams.setOriginalQuestion(query.getQuestion());
        documentParams.setReReadingQuestion(query.getQuestion());
        // 默认文档范围
        String scopeType = query.getScopeType();
        documentParams.setScopeType(scopeType);
        ContextUserRecordDTO userRecord = ContextUserRecordDTO.builder()
                                                              .chatId(query.getChatId())
                                                              .scopeType(query.getScopeType())
                                                              .businessType(documentParams.getBusinessType())
                                                              .question(query.getQuestion())
                                                              .mediaInfo(CollUtil.isNotEmpty(query.getMediaList()) ? JSONUtil.toJsonStr(query.getMediaList()) : null)
                                                              .build();
        userRecordService.insert(userRecord);
        // 使用日志 Advisor
        PromptLoggerAdvisor promptLogger = chatClientAdvisorFactory.createPromptLoggerAdvisor(null);
        List<Advisor> dataResourceAdvisors = CollUtil.newArrayList(promptLogger);

        List<Message> existingMessages = messageWindowChatMemory.get(query.getChatId().toString());
        if (CollUtil.isEmpty(existingMessages)) {
            existingMessages = chatHistoryService.loadHistoryFromDb(query.getChatId());
        }
        ChatConfigResult chatConfig = new ChatConfigResult(query.getQuestion(), userRecord, existingMessages, dataResourceAdvisors);

        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(UserChatPromptUtils.generatePromptUserSpecConsumer(query))
                .messages(chatConfig.getExistingMessages())
                .advisors(dataResourceAdvisors)
                .advisors(getAdvisorSpecConsumer(query.getChatId()))
                .call()
                .chatResponse();

        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
            userRecordService.updateAnswerById(chatConfig.getUserRecord().getId(), content);
        }
        if (log.isDebugEnabled()) {
            //频度最高的调用 使用debug级别
            log.debug("content: {}", content);
        }
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        if (chatResponse == null) {
            return "请求异常";
        }
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（支持多轮对话记忆，SSE 流式传输）
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        return commonChatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 和 RAG 知识库进行对话（使用混合检索增强）
     * scopeType 对应知识库文档范围，理论最佳实践应该是有一个本地微调模型，能将用户问题归类，即根据不同场景选择不同的知识库
     */
    public String doChatWithRag(String scopeType, Long groupId, String message, @NonNull Long chatId) {
        return doChatWithRag(scopeType, groupId, message, chatId, null);
    }

    /**
     * 和 RAG 知识库进行对话（支持多模态）
     *
     * @param mediaList 多媒体附件列表，可为null
     */
    public String doChatWithRag(String scopeType, Long groupId, String message, @NonNull Long chatId, List<MediaAttachment> mediaList) {
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);

        // 默认文档范围
        scopeType = StrUtil.blankToDefault(scopeType, "用户客服");

        // 意图识别，根据意图查询知识库、或者根据意图调用工具
        IntentResult intentResult = intentAnalysisAgent.analyzeIntent(chatId, scopeType, message);

        // 构建文档查询上下文
        DocumentQueryContext documentQueryContext = new DocumentQueryContext();
        documentQueryContext.setScopeType(scopeType);
        documentQueryContext.setGroupId(groupId);
        documentQueryContext.setBusinessType(intentResult.getBusinessType());
        documentQueryContext.setOriginalQuestion(message);
        documentQueryContext.setReReadingQuestion(rewrittenMessage);

        // 创建 RAG 请求上下文
        RequestRagContext requestRagContext = new RequestRagContext();
        requestRagContext.setChatId(chatId);

        // 记录用户对话
        ContextUserRecordDTO userRecord = ContextUserRecordDTO.builder()
                                                              .chatId(chatId)
                                                              .groupId(groupId)
                                                              .scopeType(scopeType)
                                                              .businessType(intentResult.getBusinessType())
                                                              .question(message)
                                                              .mediaInfo(CollUtil.isNotEmpty(mediaList) ? JSONUtil.toJsonStr(mediaList) : null)
                                                              .build();
        userRecordService.insert(userRecord);

        List<Message> existingMessages = messageWindowChatMemory.get(chatId.toString());
        if (existingMessages.isEmpty()) {
            existingMessages = chatHistoryService.loadHistoryFromDb(chatId);
        }
        log.info("###### Chat memory for {}: {} messages size", chatId, existingMessages.size());

        // 使用混合检索增强顾问
        HybridRetrievalAdvisor hybridAdvisor = chatClientAdvisorFactory.createHybridRetrievalAdvisor(
                documentQueryContext, intentResult, requestRagContext);

        // 构建多模态user prompt
        Consumer<ChatClient.PromptUserSpec> userSpecConsumer = UserChatPromptUtils.generatePromptUserSpecConsumer(
                buildKnowledgeParam(message, chatId, mediaList)
        );

        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(userSpecConsumer)
                .messages(existingMessages)
                .toolCallbacks(mcpToolCallbacks.getToolCallbacks())
                .toolCallbacks(ragTools)
                .advisors(getAdvisorSpecConsumer(chatId))
                .advisors(hybridAdvisor)
                .call()
                .chatResponse();

        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
            userRecordService.updateAnswerById(userRecord.getId(), content);
        }
        if (log.isDebugEnabled()) {
            // 频度最高的调用 使用 debug 级别
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

    @NotNull
    private static Consumer<ChatClient.AdvisorSpec> getAdvisorSpecConsumer(@NotNull Long chatId) {
        return spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId);
    }

}
