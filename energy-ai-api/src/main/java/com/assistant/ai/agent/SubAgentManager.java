package com.assistant.ai.agent;

import cn.hutool.core.util.StrUtil;
import com.assistant.ai.tools.InnerTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 子代理管理器
 * 管理 SubAgent 的生命周期（创建 -> 多轮对话 -> 销毁）
 * 通过 InnerTool 接口将 3 个工具暴露给主 Agent：
 * - create_sub_agent：创建 SubAgent 并执行首个任务
 * - chat_with_sub_agent：与已有 SubAgent 继续对话
 * - destroy_sub_agent：销毁 SubAgent，释放资源
 *
 * @author endcy
 * @date 2026/04/22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubAgentManager implements InnerTool {

    private final Map<String, SubAgent> subAgents = new ConcurrentHashMap<>();
    private final ChatClient commonChatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 根据 ID 获取 SubAgent
     */
    public SubAgent getSubAgent(String agentId) {
        return subAgents.get(agentId);
    }

    /**
     * 创建 SubAgent 并执行首个任务
     */
    public String createAndExecute(String name, String systemPrompt, String task) {
        SubAgent subAgent = new SubAgent(name, systemPrompt, commonChatClient);
        subAgents.put(subAgent.getId(), subAgent);
        log.info("SubAgent registered: id={}, name={}", subAgent.getId(), name);
        return subAgent.execute(task);
    }

    /**
     * 与 SubAgent 继续对话
     */
    public String chatWithSubAgent(String agentId, String message) {
        SubAgent subAgent = subAgents.get(agentId);
        if (subAgent == null) {
            return "SubAgent 不存在: " + agentId;
        }
        if (subAgent.isDestroyed()) {
            subAgents.remove(agentId);
            return "SubAgent 已被销毁: " + agentId;
        }
        return subAgent.chat(message);
    }

    /**
     * 销毁 SubAgent
     */
    public String destroySubAgent(String agentId) {
        SubAgent subAgent = subAgents.remove(agentId);
        if (subAgent == null) {
            return "SubAgent 不存在: " + agentId;
        }
        subAgent.destroy();
        return "SubAgent 已销毁: " + agentId;
    }

    /**
     * 列出所有活跃的 SubAgent
     */
    public String listSubAgents() {
        if (subAgents.isEmpty()) {
            return "当前没有活跃的 SubAgent";
        }
        StringBuilder sb = new StringBuilder("活跃的 SubAgent 列表:\n");
        for (Map.Entry<String, SubAgent> entry : subAgents.entrySet()) {
            SubAgent agent = entry.getValue();
            sb.append(String.format("- ID: %s, 名称: %s, 记忆条数: %d\n",
                    agent.getId(), agent.getName(), agent.getMemorySize()));
        }
        return sb.toString();
    }

    @Override
    public List<ToolCallback> loadToolCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();

        // 创建子代理工具
        callbacks.add(FunctionToolCallback.builder("create_sub_agent",
                                                  (String input) -> {
                                                      String name = extractField(input, "name");
                                                      String systemPrompt = extractField(input, "system_prompt");
                                                      String task = extractField(input, "task");
                                                      if (StrUtil.isBlank(name))
                                                          name = "unnamed-sub-agent";
                                                      if (StrUtil.isBlank(task))
                                                          return "任务不能为空";
                                                      return createAndExecute(name, StrUtil.blankToDefault(systemPrompt, "你是一个专业的 AI 助手"), task);
                                                  })
                                          .description("创建一个拥有独立记忆的子代理，并执行首个任务。参数：name(名称), system_prompt(系统提示词), task(首个任务)")
                                          .build());

        // 与子代理对话工具
        callbacks.add(FunctionToolCallback.builder("chat_with_sub_agent",
                                                  (String input) -> {
                                                      String agentId = extractField(input, "agent_id");
                                                      String message = extractField(input, "message");
                                                      if (StrUtil.isBlank(agentId))
                                                          return "agent_id 不能为空";
                                                      if (StrUtil.isBlank(message))
                                                          return "message 不能为空";
                                                      return chatWithSubAgent(agentId, message);
                                                  })
                                          .description("与已有子代理继续对话。参数：agent_id(子代理 ID), message(消息内容)")
                                          .build());

        // 销毁子代理工具
        callbacks.add(FunctionToolCallback.builder("destroy_sub_agent",
                                                  (String input) -> {
                                                      String agentId = extractField(input, "agent_id");
                                                      if (StrUtil.isBlank(agentId))
                                                          return "agent_id 不能为空";
                                                      return destroySubAgent(agentId);
                                                  })
                                          .description("销毁子代理，释放资源。参数：agent_id(子代理 ID)")
                                          .build());

        log.info("Registered {} SubAgent tools", callbacks.size());
        return callbacks;
    }

    private String extractField(String input, String fieldName) {
        if (input == null)
            return null;
        // 尝试解析为 JSON
        if (input.trim().startsWith("{")) {
            try {
                var json = objectMapper.readTree(input.trim());
                var node = json.get(fieldName);
                return node != null ? node.asText() : null;
            } catch (Exception e) {
                // 如果不是合法 JSON，按 key=value 格式解析
            }
        }
        // key=value 格式
        String prefix = fieldName + "=";
        int start = input.indexOf(prefix);
        if (start >= 0) {
            start += prefix.length();
            int end = input.indexOf(",", start);
            if (end == -1)
                end = input.length();
            return input.substring(start, end).trim();
        }
        return null;
    }
}
