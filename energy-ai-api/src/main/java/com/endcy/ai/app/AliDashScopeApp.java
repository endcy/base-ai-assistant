package com.endcy.ai.app;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.dashscope.app.Application;
import com.alibaba.dashscope.app.ApplicationParam;
import com.alibaba.dashscope.app.ApplicationResult;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.endcy.ai.config.ChatRagProperties;
import com.endcy.ai.mcp.config.McpConfig;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

/**
 * 阿里百炼在线应用调用。
 * AI 应用对话（支持多轮对话记忆），基于百炼的 dashscope 云产品。
 *
 * @author endcy
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Import(McpConfig.class)
public class AliDashScopeApp {

    private final DashScopeConnectionProperties dashScopeProperties;
    private final ChatRagProperties chatRagProperties;

    /**
     * AI 应用对话（支持多轮对话记忆）。
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
