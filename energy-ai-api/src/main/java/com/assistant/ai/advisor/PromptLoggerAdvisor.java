package com.assistant.ai.advisor;

import com.assistant.ai.constant.EnergyAiConstant;
import com.assistant.ai.domain.context.RequestRagContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 自定义日志 Advisor
 * 打印 info 级别日志、只输出单次用户提示词和 AI 回复的文本
 * 每次请求创建新实例，避免并发问题
 */
@Slf4j
public class PromptLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RequestRagContext requestRagContext;

    public PromptLoggerAdvisor(RequestRagContext requestRagContext) {
        this.requestRagContext = requestRagContext;
    }

    @NotNull
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return EnergyAiConstant.LOGGER_ADVISOR_ORDER;
    }

    @NotNull
    @Override
    public ChatClientResponse adviseCall(@NotNull ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        // 在调用链开始前记录原始用户输入
        String originalUserInput = extractOriginalUserInput(chatClientRequest);
        log.info("User Request: {}", originalUserInput);

        ChatClientResponse response = chain.nextCall(chatClientRequest);
        ChatResponse chatResponse = response.chatResponse();
        // 记录 AI 响应
        String aiResponse = chatResponse != null ?
                chatResponse.getResult().getOutput().getText() : null;
        log.info("AI Response: {}", aiResponse);

        // 记录 Token 用量
        logTokenUsage(chatResponse);

        return response;
    }

    /**
     * 提取原始用户输入
     */
    private String extractOriginalUserInput(ChatClientRequest request) {
        List<UserMessage> messages = request.prompt().getUserMessages();

        for (UserMessage message : messages) {
            if (log.isDebugEnabled()) {
                log.info(">>>>>> User Message and Rag Context: {}", message);
            }
        }

        // 从消息列表中找到最新的用户消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message != null) {
                return message.getText();
            }
        }

        // 备选方案：直接从请求参数中获取
        return request.prompt().getUserMessage() != null ?
                request.prompt().getUserMessage().getText() : "Unable to extract user input";
    }

    /**
     * 记录 Token 用量（结构化 JSON 日志）
     * 输入 token 包含系统提示词、RAG 上下文、用户问题等完整增强后的 prompt
     */
    private void logTokenUsage(ChatResponse chatResponse) {
        if (chatResponse == null) {
            return;
        }

        ChatResponseMetadata metadata = chatResponse.getMetadata();

        Usage usage = metadata.getUsage();
        if (usage == null) {
            return;
        }

        int promptTokens = usage.getPromptTokens();
        int completionTokens = usage.getCompletionTokens();
        int totalTokens = usage.getTotalTokens();

        // 写入 RequestRagContext（如果不为空）
        if (requestRagContext != null) {
            requestRagContext.setPromptTokens(promptTokens);
            requestRagContext.setCompletionTokens(completionTokens);
        }

        try {
            Map<String, Object> logData = new HashMap<>();
            logData.put("inputTokens", promptTokens);
            logData.put("outputTokens", completionTokens);
            logData.put("totalTokens", totalTokens);

            // 尝试获取会话 ID
            String sessionId = metadata.getId();
            if (sessionId != null) {
                logData.put("sessionId", sessionId);
            }

            // 尝试获取模型信息
            String model = metadata.getModel();
            if (model != null) {
                logData.put("model", model);
            }

            log.info("TokenUsage: {}", MAPPER.writeValueAsString(logData));
        } catch (Exception e) {
            log.warn("Failed to serialize token usage log", e);
            log.info("TokenUsage: inputTokens={}, outputTokens={}, totalTokens={}",
                    promptTokens, completionTokens, totalTokens);
        }
    }

    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(@NotNull ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        // 类似的修复用于流式处理
        String originalUserInput = extractOriginalUserInput(chatClientRequest);
        log.info("User Request: {}", originalUserInput);

        Flux<ChatClientResponse> responseFlux = chain.nextStream(chatClientRequest);

        // 流式响应需要累积最终的 ChatResponse 来获取 token 用量
        AtomicReference<ChatResponse> lastResponse = new AtomicReference<>();

        return responseFlux.doOnNext(response -> {
            if (response != null && response.chatResponse() != null) {
                String aiResponse = response.chatResponse().getResult().getOutput().getText();
                log.info("AI Response: {}", aiResponse);
                lastResponse.set(response.chatResponse());
            }
        }).doOnComplete(() -> logTokenUsage(lastResponse.get()));
    }
}
