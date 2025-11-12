package com.assistant.ai.app;

import com.assistant.ai.constant.EnergyAiConstant;
import com.assistant.ai.domain.vo.EnergyReport;
import com.assistant.ai.mcp.config.McpConfig;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@DependsOn("vectorStoreManager")
@Import(McpConfig.class)
public class EnergyAiToolsApp {

    @Resource
    @Qualifier("commonChatClient")
    private ChatClient commonChatClient;

    //MCP 图片搜索
    @Resource
    @Qualifier("imageSearchProvider")
    private ToolCallbackProvider imageSearchProvider;

    @Resource
    @Qualifier("electricityPriceSearchProvider")
    private ToolCallbackProvider electricityPriceSearchProvider;

    //AI 调用工具能力
    @Resource
    @Qualifier("commonWebTools")
    private ToolCallback[] commonWebTools;

    /**
     * AI 智慧能源报告功能 结构化输出
     */
    public EnergyReport doChatWithReport(String message, String chatId) {
        EnergyReport energyReport = commonChatClient
                .prompt()
                .system(EnergyAiConstant.SYSTEM_PROMPT + "每次对话后都要生成智慧能源结果，标题为{用户名}的智慧能源报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(EnergyReport.class);
        log.info("energyReport: {}", energyReport);
        return energyReport;
    }

    /**
     * AI 智慧能源 调用工具 调用验证
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(commonWebTools)
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
     * AI 智慧能源 调用MCP服务 调用验证
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = commonChatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(imageSearchProvider)
                .toolCallbacks(electricityPriceSearchProvider)
                .call()
                .chatResponse();
        String content = null;
        if (chatResponse != null) {
            content = chatResponse.getResult().getOutput().getText();
        }
        log.info("content: {}", content);
        return content;
    }

}
