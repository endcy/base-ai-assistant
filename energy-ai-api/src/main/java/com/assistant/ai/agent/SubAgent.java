package com.assistant.ai.agent;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 子代理（SubAgent）
 * 拥有独立的 ChatMemory 实例，与主 Agent 的记忆完全隔离
 * 共享主 Agent 的 ChatClient（即共享同一个大模型连接），但对话历史完全独立
 *
 * @author endcy
 * @date 2026/04/22
 */
@Slf4j
public class SubAgent {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    @Getter
    private final String id;

    @Getter
    private final String name;

    private final String systemPrompt;

    private final ChatClient chatClient;

    // 独立记忆
    private final List<Message> memory = new CopyOnWriteArrayList<>();

    private final int maxRounds;

    private boolean destroyed;

    public SubAgent(String name, String systemPrompt, ChatClient chatClient) {
        this(name, systemPrompt, chatClient, 10);
    }

    public SubAgent(String name, String systemPrompt, ChatClient chatClient, int maxRounds) {
        this.id = "sub-agent-" + ID_GENERATOR.incrementAndGet();
        this.name = name;
        this.systemPrompt = systemPrompt;
        this.chatClient = chatClient;
        this.maxRounds = maxRounds;
        log.info("SubAgent created: id={}, name={}", this.id, this.name);
    }

    /**
     * 执行单个任务（单次对话）
     */
    public String execute(String task) {
        if (destroyed) {
            return "SubAgent 已销毁，无法执行任务";
        }

        memory.add(new UserMessage(task));

        try {
            List<Message> messages = buildMessageList();
            Prompt prompt = new Prompt(messages);

            var response = chatClient.prompt(prompt).call().chatResponse();
            if (response != null) {
                String content = response.getResult().getOutput().getText();
                memory.add(new AssistantMessage(content));
                log.info("SubAgent {} executed task, response length: {}", id, content.length());
                return content;
            }
            return "SubAgent 执行失败：无响应";
        } catch (Exception e) {
            log.error("SubAgent {} execution error: {}", id, e.getMessage(), e);
            return "SubAgent 执行异常: " + e.getMessage();
        }
    }

    /**
     * 继续对话（多轮对话）
     */
    public String chat(String message) {
        if (destroyed) {
            return "SubAgent 已销毁，无法对话";
        }

        memory.add(new UserMessage(message));

        // 滑动窗口限制
        if (memory.size() > maxRounds * 4) {
            while (memory.size() > maxRounds * 4 - 4) {
                memory.remove(0);
            }
        }

        try {
            Prompt prompt = new Prompt(buildMessageList());
            var response = chatClient.prompt(prompt).call().chatResponse();
            if (response != null) {
                String content = response.getResult().getOutput().getText();
                memory.add(new AssistantMessage(content));
                return content;
            }
            return "SubAgent 无响应";
        } catch (Exception e) {
            log.error("SubAgent {} chat error: {}", id, e.getMessage(), e);
            return "SubAgent 对话异常: " + e.getMessage();
        }
    }

    /**
     * 销毁 SubAgent，释放记忆
     */
    public void destroy() {
        this.destroyed = true;
        memory.clear();
        log.info("SubAgent destroyed: id={}, name={}", id, name);
    }

    private List<Message> buildMessageList() {
        return List.copyOf(memory);
    }

    public int getMemorySize() {
        return memory.size();
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
