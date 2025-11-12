package com.assistant.ai.app;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.assistant.ai.config.ChatRagProperties;
import com.assistant.ai.mcp.config.McpConfig;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@DependsOn("vectorStoreManager")
@Import(McpConfig.class)
public class EnergyAiAliDashScopeApp {

    private final DashScopeConnectionProperties dashScopeProperties;
    private final ChatRagProperties chatRagProperties;

    /**
     * AI 应用对话（支持多轮对话记忆）
     * 基于百炼的dashscope云产品
     */
    public String doAppChat(String message, String chatId) {
        ApplicationParam param = ApplicationParam.builder()
                                                 .apiKey(dashScopeProperties.getApiKey())
                                                 .appId(chatRagProperties.getAliDashScopeAppId())
                                                 .prompt(message)
                                                 .incrementalOutput(true)
                                                 .sessionId(chatId)
                                                 .build();
        Application application = new Application();
        //流式输出内容
        Flowable<ApplicationResult> result;
        try {
            result = application.streamCall(param);
        } catch (NoApiKeyException | InputRequiredException e) {
            throw new RuntimeException(e);
        }
        StringBuilder fullContent = new StringBuilder();
        try {
            result.blockingForEach(data -> {
                String text = data.getOutput().getText();
                fullContent.append(text);
            });
        } catch (Exception e) {
            log.error("Error while processing stream response", e);
            throw e;
        }

        String finalContent = fullContent.toString();
        log.info("Final content: {}", finalContent);
        return finalContent;
    }

}
