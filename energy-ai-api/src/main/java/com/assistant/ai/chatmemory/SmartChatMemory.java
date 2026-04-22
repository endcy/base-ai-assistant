package com.assistant.ai.chatmemory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 带三层上下文压缩策略的对话记忆
 * 第一层：摘要压缩 - 历史消息超过 16 条时自动压缩为摘要
 * 第二层：Assistant 消息裁剪 - 只保留最近 3 条 Assistant 回复
 * 第三层：滑动窗口 - 消息总数超过 maxRounds * 4 时丢弃最早消息
 *
 * @author endcy
 * @date 2026/04/22
 */
@Slf4j
public class SmartChatMemory implements ChatMemory {

    private static final int COMPRESS_THRESHOLD = 16;
    private static final int PRESERVE_RECENT_MESSAGES = 6;
    private static final int MAX_ASSISTANT_MESSAGES = 3;
    private static final int SUMMARY_MAX_WORDS = 300;

    private final ChatClient chatClient;
    private final Map<String, SessionMemory> sessions = new ConcurrentHashMap<>();

    public SmartChatMemory(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        SessionMemory session = sessions.computeIfAbsent(conversationId, k -> new SessionMemory());
        session.history.addAll(messages);
    }

    @Override
    public List<Message> get(String conversationId) {
        SessionMemory session = sessions.computeIfAbsent(conversationId, k -> new SessionMemory());
        compressIfNeeded(session);
        trimAssistantMessages(session);
        applySlidingWindow(session);
        return buildMessageList(session);
    }

    @Override
    public void clear(String conversationId) {
        sessions.remove(conversationId);
    }

    private void compressIfNeeded(SessionMemory session) {
        if (chatClient == null || session.history.size() <= COMPRESS_THRESHOLD) {
            return;
        }

        int compressEndIndex = session.history.size() - PRESERVE_RECENT_MESSAGES;

        // 确保不会在 TOOL 消息前面截断
        while (compressEndIndex < session.history.size()
                && isToolRelatedMessage(session.history.get(compressEndIndex))) {
            compressEndIndex--;
        }

        if (compressEndIndex <= 0) {
            return;
        }

        List<Message> messagesToCompress = new ArrayList<>(session.history.subList(0, compressEndIndex));
        String newSummary = compressMessages(messagesToCompress, session.summary);

        if (newSummary != null && !newSummary.isBlank()) {
            session.summary = newSummary;
            session.history.subList(0, compressEndIndex).clear();
            log.debug("Chat memory compressed. Summary length: {}, remaining history: {}",
                    session.summary.length(), session.history.size());
        }
    }

    private void trimAssistantMessages(SessionMemory session) {
        int assistantCount = 0;
        List<Message> toRemove = new ArrayList<>();

        for (int i = session.history.size() - 1; i >= 0; i--) {
            Message msg = session.history.get(i);
            if (msg instanceof AssistantMessage) {
                assistantCount++;
                if (assistantCount > MAX_ASSISTANT_MESSAGES) {
                    toRemove.add(msg);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            session.history.removeAll(toRemove);
            log.debug("Trimmed {} old assistant messages", toRemove.size());
        }
    }

    private void applySlidingWindow(SessionMemory session) {
        int maxMessages = 40;
        while (session.history.size() > maxMessages) {
            session.history.remove(0);
        }
    }

    private List<Message> buildMessageList(SessionMemory session) {
        List<Message> messages = new ArrayList<>();

        // 将原始 system prompt 与摘要合并为一条 SystemMessage
        if (session.systemMessage != null || (session.summary != null && !session.summary.isBlank())) {
            String systemContent = session.systemMessage != null ? session.systemMessage.getText() : "";
            if (session.summary != null && !session.summary.isBlank()) {
                systemContent += "\n\n【以下是之前对话的摘要，请参考】\n" + session.summary;
            }
            messages.add(new SystemMessage(systemContent));
        }

        // 添加历史消息，跳过早期被裁剪的 Assistant 消息
        for (Message msg : session.history) {
            messages.add(msg);
        }

        return List.copyOf(messages);
    }

    private String compressMessages(List<Message> messages, String existingSummary) {
        try {
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("请将以下对话历史压缩为一段简洁的摘要（不超过").append(SUMMARY_MAX_WORDS).append("字），保留关键信息和上下文：\n\n");

            if (existingSummary != null && !existingSummary.isBlank()) {
                promptBuilder.append("【已有历史摘要】\n").append(existingSummary).append("\n\n");
            }

            for (Message msg : messages) {
                String role = switch (msg.getMessageType()) {
                    case USER -> "用户";
                    case ASSISTANT -> "助手";
                    case SYSTEM -> "系统";
                    default -> msg.getMessageType().toString();
                };
                promptBuilder.append(role).append(": ").append(msg.getText()).append("\n\n");
            }

            Prompt prompt = new Prompt(promptBuilder.toString());
            var response = chatClient.prompt(prompt).call().content();

            if (response != null && response.length() > SUMMARY_MAX_WORDS) {
                response = response.substring(0, SUMMARY_MAX_WORDS);
            }
            return response;
        } catch (Exception e) {
            log.warn("Failed to compress chat memory: {}", e.getMessage());
            return existingSummary;
        }
    }

    private boolean isToolRelatedMessage(Message msg) {
        return msg instanceof AssistantMessage assistantMsg
                && assistantMsg.getToolCalls() != null
                && !assistantMsg.getToolCalls().isEmpty();
    }

    public void setSystemPrompt(String conversationId, String systemPrompt) {
        SessionMemory session = sessions.computeIfAbsent(conversationId, k -> new SessionMemory());
        session.systemMessage = new SystemMessage(systemPrompt);
    }

    public int getHistorySize(String conversationId) {
        SessionMemory session = sessions.get(conversationId);
        return session != null ? session.history.size() : 0;
    }

    private static class SessionMemory {
        List<Message> history = new ArrayList<>();
        String summary;
        SystemMessage systemMessage;
    }
}
