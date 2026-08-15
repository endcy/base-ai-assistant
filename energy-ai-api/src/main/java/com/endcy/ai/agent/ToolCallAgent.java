package com.endcy.ai.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.endcy.ai.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base agent class for handling tool calls, with concrete implementations of think and act methods.
 * Can be used as a parent class for creating instances.
 *
 * @author endcy
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // Available tools
    private final ToolCallback[] availableTools;
    // Tool calling manager
    private final ToolCallingManager toolCallingManager;
    // Disable Spring AI's built-in tool calling mechanism; manage options and message context ourselves
    private final ChatOptions chatOptions;
    // Chat response containing tool call info (which tools to call)
    private ChatResponse toolCallChatResponse;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // Disable Spring AI's built-in tool calling mechanism; manage options and message context ourselves
        this.chatOptions = DashScopeChatOptions.builder()
                                               .withInternalToolExecutionEnabled(false)
                                               .build();
    }

    /**
     * Process current state and decide the next action.
     *
     * @return true if action should be taken
     */
    @Override
    public boolean think() {
        // 1. Validate prompt, append user prompt
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        // 2. Call AI model, get tool call results
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, this.chatOptions);
        try {
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                                                       .system(getSystemPrompt())
                                                       .tools(availableTools)
                                                       .call()
                                                       .chatResponse();
            // Record response for later Act step
            this.toolCallChatResponse = chatResponse;
            // 3. Parse tool call results, get tools to invoke
            // Assistant message
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // Get list of tools to call
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // Output info
            String result = assistantMessage.getText();
            log.info(getName() + "的思考：" + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                                              .map(toolCall -> String.format("工具名称：%s，参数：%s", toolCall.name(), toolCall.arguments()))
                                              .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            // If no tool calls needed, return false
            if (toolCallList.isEmpty()) {
                // Only manually record assistant message when no tool call is made
                getMessageList().add(assistantMessage);
                return false;
            } else {
                // When tools need to be called, no need to manually record assistant message
                // because it will be auto-recorded during tool execution
                return true;
            }
        } catch (Exception e) {
            log.error("{}" + "的思考过程遇到问题", getName(), e);
            getMessageList().add(new AssistantMessage("处理时遇到了错误：" + e.getMessage()));
            return false;
        }
    }

    /**
     * Execute tool calls and process results.
     *
     * @return execution result
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具需要调用";
        }
        // Call tools
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // Record message context; conversationHistory already includes assistant message and tool call result
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // Check if the termination tool was called
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                                                         .anyMatch(response -> response.name().equals("doTerminate"));
        if (terminateToolCalled) {
            // Task ended, update state
            setState(AgentState.FINISHED);
        }
        String results = toolResponseMessage.getResponses().stream()
                                            .map(response -> "工具 " + response.name() + " 返回的结果：" + response.responseData())
                                            .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }
}
