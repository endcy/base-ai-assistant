package com.assistant.ai.config;

import cn.hutool.core.util.ArrayUtil;
import com.assistant.ai.advisor.ChatClientAdvisorFactory;
import com.assistant.ai.constant.EnergyAiConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatClient相关定义
 *
 * @author endcy
 * @date 2025/10/31 20:41:31
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class ChatClientConfig {

    /**
     * 支持启用本地ollama运行的大模型
     */
//    private final ChatModel ollamaChatModel;

    private final ChatModel dashscopeChatModel;
    private final SyncMcpToolCallbackProvider mcpToolCallbacks;
    private final ChatClientAdvisorFactory chatClientAdvisorFactory;


    @Bean("commonChatClient")
    public ChatClient commonChatClient() {
        ToolCallback[] tools = mcpToolCallbacks.getToolCallbacks();
        if (ArrayUtil.isEmpty(tools)) {
            log.warn("########## No MCP tools registered");
        } else {
            log.info("########## MCP tools registered: {}", tools.length);
        }
        return ChatClient.builder(dashscopeChatModel)
                         .defaultSystem(EnergyAiConstant.SYSTEM_PROMPT)
                         .defaultAdvisors(
                                 chatClientAdvisorFactory.getMessageChatMemoryAdvisor(),
                                 chatClientAdvisorFactory.getPromptLoggerAdvisor(),
                                 chatClientAdvisorFactory.getReReadingAdvisor()
                         )
                         .build();
    }

    @Bean("intentChatClient")
    public ChatClient intentChatClient() {
        //暂无微调模型 使用云端模型  意图识别时不用打印过多日志，不用其他顾问
        return ChatClient.builder(dashscopeChatModel).build();
    }

}
