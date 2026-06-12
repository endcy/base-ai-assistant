package com.assistant.ai.app;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.assistant.ai.advisor.ChatClientAdvisorFactory;
import com.assistant.ai.advisor.PromptLoggerAdvisor;
import com.assistant.ai.agent.IntentAnalysisAgent;
import com.assistant.ai.agent.model.IntentResult;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.domain.context.RequestRagContext;
import com.assistant.ai.mcp.config.McpConfig;
import com.assistant.ai.rag.QueryRewriter;
import com.assistant.ai.repository.domain.context.DocumentQueryContext;
import com.assistant.ai.repository.domain.dto.ContextUserRecordDTO;
import com.assistant.ai.repository.service.ContextUserRecordService;
import com.assistant.ai.rpc.domain.base.AIStreamResponse;
import com.assistant.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.assistant.ai.rpc.domain.response.KnowledgeDocumentMatchItem;
import com.assistant.ai.rpc.enums.ApiQaType;
import com.assistant.ai.rpc.enums.MessageType;
import com.assistant.ai.tools.DeepSeekWebSearchTool;
import com.assistant.ai.util.DocumentConvertUtils;
import com.assistant.service.common.exception.CoException;
import com.assistant.service.domain.enums.PossibleSourceTypeEnum;
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
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

/**
 * 能源 AI 文档应用服务类
 * 提供简单问答、RAG 知识库问答、DeepSeek 搜索以及流式对话功能
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
    //对话提示词重写器
    private final QueryRewriter queryRewriter;
    //通用顾问工厂
    private final ChatClientAdvisorFactory chatClientAdvisorFactory;
    private final ContextUserRecordService userRecordService;
    private final MessageWindowChatMemory messageWindowChatMemory;
    private final DeepSeekWebSearchTool deepSeekWebSearchTool;

    private final SyncMcpToolCallbackProvider mcpToolCallbacks;
    private final ToolCallback[] ragTools;
    private final IntentAnalysisAgent intentAnalysisAgent;
    private final ChatRagProperties chatRagProperties;

    private record ChatConfigResult(String rewrittenMessage,
                                    ContextUserRecordDTO userRecord,
                                    List<Message> existingMessages,
                                    List<Advisor> dataResourceAdvisors) {
    }


    /**
     * AI 简单进行问答
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
                                                              .build();
        userRecordService.insert(userRecord);
        // 使用日志 Advisor
        PromptLoggerAdvisor promptLogger = chatClientAdvisorFactory.createPromptLoggerAdvisor(null);
        List<Advisor> dataResourceAdvisors = CollUtil.newArrayList(promptLogger);

        List<Message> existingMessages = messageWindowChatMemory.get(query.getChatId().toString());
        ChatConfigResult chatConfig = new ChatConfigResult(query.getQuestion(), userRecord, existingMessages, dataResourceAdvisors);

        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(query.getQuestion())
                .advisors(dataResourceAdvisors)
                .advisors(getAdvisorSpecConsumer(query.getChatId()))
                .call()
                .chatResponse();

        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
            userRecordService.updateAnswerById(chatConfig.userRecord().getId(), content);
        }
        if (log.isDebugEnabled()) {
            //频度最高的调用 使用debug级别
            log.debug("content: {}", content);
        }
        return content;
    }

    /**
     * AI RAG 知识库进行对话
     * scopeType对应知识库文档范围，理论最佳实践应该是有一个本地微调模型，能将用户问题归类，即根据不同场景选择不同的知识库
     */
    public String doChatRag(KnowledgeAIQueryParam query, RequestRagContext requestRagContext) {
        ChatConfigResult chatConfig = chatClientConfig(query, requestRagContext);

        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(chatConfig.rewrittenMessage())
                .messages(chatConfig.existingMessages())
                .advisors(getAdvisorSpecConsumer(query.getChatId()))
                .advisors(chatConfig.dataResourceAdvisors())
                .call()
                .chatResponse();

        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
            userRecordService.updateAnswerById(chatConfig.userRecord().getId(), content);
        }
        if (log.isDebugEnabled()) {
            //频度最高的调用 使用debug级别
            log.debug("content: {}", content);
        }
        return content;
    }

    /**
     * deepseek搜索
     */
    public String deepseek(KnowledgeAIQueryParam query) {
        ContextUserRecordDTO userRecord = ContextUserRecordDTO.builder()
                                                              .chatId(query.getChatId())
                                                              .scopeType(query.getScopeType())
                                                              .businessType(query.getBusinessType())
                                                              .question(query.getQuestion())
                                                              .build();
        userRecordService.insert(userRecord);
        String content;
        try {
            content = deepSeekWebSearchTool.searchQuestion(null, query.getQuestion());
        } catch (Exception e) {
            log.error("deepseek search error", e);
            content = "deepseek search error " + e.getMessage();
        }
        userRecordService.updateAnswerById(userRecord.getId(), content);
        if (log.isDebugEnabled()) {
            //频度最高的调用 使用debug级别
            log.debug("content: {}", content);
        }
        return content;
    }

    @NotNull
    private ChatConfigResult chatClientConfig(KnowledgeAIQueryParam query, RequestRagContext requestRagContext) {
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(query.getQuestion());
        DocumentQueryContext documentParams = new DocumentQueryContext();
        documentParams.setOriginalQuestion(query.getQuestion());
        documentParams.setReReadingQuestion(rewrittenMessage);
        // 默认文档范围
        String scopeType = StrUtil.blankToDefault(query.getScopeType(), "用户客服");
        documentParams.setScopeType(scopeType);
        if (query != null) {
        }

        // 意图识别，根据意图查询知识库、或者根据意图调用工具 当前外部仅DB知识库，暂不使用
        IntentResult intentResult = new IntentResult();
        intentResult.setChatId(query.getChatId());
        intentResult.setScopeType(query.getScopeType());
        intentResult.setUserMessage(rewrittenMessage);
        intentResult.setDataScopeList(CollUtil.newArrayList(PossibleSourceTypeEnum.VECTOR));

        ContextUserRecordDTO userRecord = ContextUserRecordDTO.builder()
                                                              .chatId(query.getChatId())
                                                              .scopeType(query.getScopeType())
                                                              .businessType(documentParams.getBusinessType())
                                                              .question(query.getQuestion())
                                                              .build();
        userRecordService.insert(userRecord);

        List<Message> existingMessages = messageWindowChatMemory.get(query.getChatId().toString());
        log.info("###### Chat memory for {}: {} messages size", query.getChatId(), existingMessages.size());

        // 使用日志 Advisor
        PromptLoggerAdvisor promptLogger = chatClientAdvisorFactory.createPromptLoggerAdvisor(null);

        List<Advisor> dataResourceAdvisors = CollUtil.newArrayList(promptLogger);
        ApiQaType qaType = ApiQaType.getByCode(query.getQueryType());
        if (qaType == ApiQaType.RAG) {
            dataResourceAdvisors.add(chatClientAdvisorFactory.createHybridRetrievalAdvisor(documentParams, intentResult, requestRagContext));
        }
        return new ChatConfigResult(rewrittenMessage, userRecord, existingMessages, dataResourceAdvisors);
    }


    @NotNull
    private static Consumer<ChatClient.AdvisorSpec> getAdvisorSpecConsumer(@NotNull Long chatId) {
        return spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId);
    }


    /**
     * AI RAG 知识库进行对话，SSE流式传输
     */
    public Flux<AIStreamResponse> doChatRagStream(KnowledgeAIQueryParam query, RequestRagContext requestRagContext) {
        ChatConfigResult chatConfig = chatClientConfig(query, requestRagContext);

        Flux<AIStreamResponse> textStream = commonChatClient
                .prompt()
                .user(chatConfig.rewrittenMessage())
                .messages(chatConfig.existingMessages())
                .advisors(getAdvisorSpecConsumer(query.getChatId()))
                .advisors(chatConfig.dataResourceAdvisors())
                .stream()
                .chatResponse()
                .map(chatResponse -> {
                    String text = chatResponse != null
                            ? chatResponse.getResult().getOutput().getText() : "";
                    return createTextChunkResponse(query.getChatId(), text);
                })
                .doOnNext(resp -> resp.setFinal(false))
                .onErrorMap(throwable -> new CoException("AI服务调用失败", throwable));

        // 文本流结束后，依次发送文档响应和 token量响应
        Mono<AIStreamResponse> documentMono = Mono.fromCallable(() ->
                createDocumentChunkResponse(query.getChatId(), requestRagContext)
        );

        Mono<AIStreamResponse> tokenMono = Mono.fromCallable(() ->
                createTokenChunkResponse(query.getChatId(), requestRagContext)
        );

        // 合并流：先发送文本流，再合并发送文档响应，最后发送token量响应
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
        // 2. 创建文档流
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

}
