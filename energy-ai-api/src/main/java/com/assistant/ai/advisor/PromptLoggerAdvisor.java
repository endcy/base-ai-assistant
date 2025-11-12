package com.assistant.ai.advisor;

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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 自定义日志 Advisor
 * 打印 info 级别日志、只输出单次用户提示词和 AI 回复的文本
 */
@Component
@Slf4j
public class PromptLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    @NotNull
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 0;
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

        return response;
    }

    /**
     * 提取原始用户输入
     */
    private String extractOriginalUserInput(ChatClientRequest request) {
        List<UserMessage> messages = request.prompt().getUserMessages();

        for (UserMessage message : messages) {
            log.info(">>>>>> User Message: {}", message);
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

    @NotNull
    @Override
    public Flux<ChatClientResponse> adviseStream(@NotNull ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        // 类似的修复用于流式处理
        String originalUserInput = extractOriginalUserInput(chatClientRequest);
        log.info("User Request: {}", originalUserInput);

        Flux<ChatClientResponse> responseFlux = chain.nextStream(chatClientRequest);

        return responseFlux.doOnNext(response -> {
            if (response != null && response.chatResponse() != null) {
                String aiResponse = response.chatResponse().getResult().getOutput().getText();
                log.info("AI Response: {}", aiResponse);
            }
        });
    }
}
