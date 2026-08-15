package com.endcy.ai.workflow.nodes;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.endcy.ai.workflow.engine.VariablePool;
import com.endcy.ai.workflow.engine.WorkflowNode;
import org.springframework.ai.chat.client.ChatClient;

/**
 * LLM node — calls the large language model to generate text.
 *
 * <p>Reads input from the variable pool (defaults to the previous node's output), calls the LLM, writes output to pool.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
public class LlmNode extends WorkflowNode {

    private final DashScopeChatModel chatModel;
    private final String systemPrompt;
    private final String inputSelector; // nodeId to read input from (null uses sys:query)

    public LlmNode(String nodeId, DashScopeChatModel chatModel, String systemPrompt, String inputSelector) {
        super(nodeId, "LLM");
        this.chatModel = chatModel;
        this.systemPrompt = systemPrompt;
        this.inputSelector = inputSelector;
    }

    @Override
    public NodeResult run(VariablePool pool) {
        String input;
        if (StrUtil.isNotBlank(inputSelector)) {
            input = pool.get(inputSelector, "output");
        } else {
            input = pool.get(VariablePool.SYS, "query");
        }
        if (input == null) {
            return NodeResult.failure("LLM 节点无输入");
        }

        try {
            String output = ChatClient.builder(chatModel)
                                      .build()
                                      .prompt()
                                      .system(StrUtil.blankToDefault(systemPrompt, "你是智慧能源AI助手"))
                                      .user(input)
                                      .call()
                                      .content();
            return NodeResult.success(output);
        } catch (Exception e) {
            return NodeResult.failure("LLM 调用失败: " + e.getMessage());
        }
    }
}
