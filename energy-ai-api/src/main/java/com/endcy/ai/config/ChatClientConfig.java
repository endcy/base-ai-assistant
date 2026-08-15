package com.endcy.ai.config;

import cn.hutool.core.util.ArrayUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.endcy.ai.advisor.ChatClientAdvisorFactory;
import com.endcy.ai.constant.EnergyAiConstant;
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
                                 chatClientAdvisorFactory.getReReadingAdvisor()
                         )
                         .build();
    }

    @Bean("simpleChatClient")
    public ChatClient simpleChatClient() {
        return ChatClient.builder(dashscopeChatModel)
                         .defaultAdvisors(
                                 chatClientAdvisorFactory.getMessageChatMemoryAdvisor()
                         )
                         .build();
    }

    @Bean("intentChatClient")
    public ChatClient intentChatClient() {
        return ChatClient.builder(dashscopeChatModel).build();
    }

    /**
     * 多媒体分析专用 ChatClient，使用可配置的多模态模型（默认 qwen3.5-omni-flash）
     * 复用 dashscopeChatModel，通过 DashScopeChatOptions 按需切换模型
     */
    @Bean("mediaAnalysisChatClient")
    public ChatClient mediaAnalysisChatClient(ChatRagProperties chatRagProperties) {
        DashScopeChatOptions mediaOptions = DashScopeChatOptions.builder()
                                                                .withModel(chatRagProperties.getMediaAnalysisModel())
                                                                .build();
        return ChatClient.builder(dashscopeChatModel)
                         .defaultOptions(mediaOptions)
                         .build();
    }

}
