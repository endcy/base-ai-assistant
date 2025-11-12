package com.assistant.ai.app;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.assistant.ai.advisor.ChatClientAdvisorFactory;
import com.assistant.ai.agent.IntentAnalysisAgent;
import com.assistant.ai.agent.model.IntentResult;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.mcp.config.McpConfig;
import com.assistant.ai.rag.QueryRewriter;
import com.assistant.ai.repository.domain.dto.ContextUserRecordDTO;
import com.assistant.ai.repository.service.ContextUserRecordService;
import com.assistant.service.domain.enums.KnowledgeBusinessTypeEnum;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
     * 和 RAG 知识库进行对话
     * scopeType对应知识库文档范围，理论最佳实践应该是有一个本地微调模型，能将用户问题归类，即根据不同场景选择不同的知识库
     */
    public String doChatWithRag(String scopeType, Long groupId, String message, @NonNull Long chatId) {
        // 查询重写
        String rewrittenMessage = queryRewriter.doQueryRewrite(message);
        Map<String, Object> expressionMap = new HashMap<>();
        // 默认文档范围
        scopeType = StrUtil.blankToDefault(scopeType, "用户客服");
        expressionMap.put("scopeType", scopeType);
        if (groupId != null) {
            expressionMap.put("groupId", groupId);
        }
        // 意图识别，根据意图查询知识库、或者根据意图调用工具
        IntentResult intentResult = intentAnalysisAgent.analyzeIntent(chatId, scopeType, message);

        KnowledgeBusinessTypeEnum businessType = KnowledgeBusinessTypeEnum.create(intentResult.getBusinessType());
        if (!KnowledgeBusinessTypeEnum.UNKNOWN.equals(businessType) && BooleanUtil.isTrue(chatRagProperties.getEnableIntentAnalysis())) {
            expressionMap.put("businessType", businessType.getType());
        }
        ContextUserRecordDTO userRecord = ContextUserRecordDTO.builder()
                                                              .chatId(chatId)
                                                              .groupId(groupId)
                                                              .scopeType(scopeType)
                                                              .businessType(businessType.getType())
                                                              .question(message)
                                                              .build();
        userRecordService.insert(userRecord);

        List<Message> existingMessages = messageWindowChatMemory.get(chatId.toString());
        log.info("###### Chat memory for {}: {} messages size", chatId, existingMessages.size());

        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(rewrittenMessage)
                .toolCallbacks(mcpToolCallbacks.getToolCallbacks())
                .toolCallbacks(ragTools)
                .advisors(getAdvisorSpecConsumer(chatId))
                // Rag文档检索增强 简单问答可关闭以减少token消耗
                .advisors(chatClientAdvisorFactory.getDocumentSimilarityAdvisor())
                // 文档向量元数据检索
                .advisors(chatClientAdvisorFactory.createAiRagFilterAdvisor(expressionMap, pgVectorVectorStore))
                // db或云文档文档检索
                .advisors(chatClientAdvisorFactory.getStoreDocumentAdvisor())
                // 本地文档检索
                .advisors(chatClientAdvisorFactory.getLocalDocumentAdvisor())
                .call()
                .chatResponse();

        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
            userRecordService.updateAnswerById(userRecord.getId(), content);
        }
        if (log.isDebugEnabled()) {
            //频度最高的调用 使用debug级别
            log.debug("content: {}", content);
        }
        return content;
    }

    @NotNull
    private static Consumer<ChatClient.AdvisorSpec> getAdvisorSpecConsumer(@NotNull Long chatId) {
        return spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId);
    }

}
