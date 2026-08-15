package com.endcy.ai.agent;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.endcy.ai.advisor.ChatClientAdvisorFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

/**
 * Smart Energy Assistant AI super-agent (with autonomous planning capability).
 *
 * @author endcy
 */
@Component
public class EnergyManus extends ToolCallAgent {

    private static final int DEFAULT_MAX_STEPS = 20;

    /**
     * Construct the Smart Energy Assistant super-agent.
     *
     * @param commonWebTools           common web tool callbacks
     * @param dashscopeChatModel       LLM model
     * @param chatClientAdvisorFactory advisor factory
     */
    public EnergyManus(ToolCallback[] commonWebTools, DashScopeChatModel dashscopeChatModel, ChatClientAdvisorFactory chatClientAdvisorFactory) {
        super(commonWebTools);
        this.setName("EnergyAIManus");
        String systemPrompt = """
                You are EnergyAIManus, an all-capable AI assistant, aimed at solving any task presented by the user.
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                """;
        this.setSystemPrompt(systemPrompt);
        String nextStepPrompt = """
                Based on user needs, proactively select the most appropriate tool or combination of tools.
                For complex tasks, you can break down the problem and use different tools step by step to solve it.
                After using each tool, clearly explain the execution results and suggest the next steps.
                If you want to stop the interaction at any point, use the `terminate` tool/function call.
                """;
        this.setNextStepPrompt(nextStepPrompt);
        this.setMaxSteps(DEFAULT_MAX_STEPS);
        // Initialize AI chat client
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                                          .defaultAdvisors(chatClientAdvisorFactory.createPromptLoggerAdvisor(null))
                                          .build();
        this.setChatClient(chatClient);
    }
}
