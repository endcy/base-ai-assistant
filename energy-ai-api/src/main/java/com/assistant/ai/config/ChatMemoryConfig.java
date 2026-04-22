package com.assistant.ai.config;

import com.assistant.ai.chatmemory.FileBasedChatMemory;
import com.assistant.ai.chatmemory.SmartChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ChatMemory相关配置
 *
 * @author endcy
 * @date 2025/10/31 20:41:31
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class ChatMemoryConfig {

    private final ChatModel dashscopeChatModel;

    @Bean("messageFileChatMemory")
    public FileBasedChatMemory messageFileChatMemory() {
        // 初始化基于文件的对话记忆
        String fileDir = "/opt/energy-ai/chat-memory";
        return new FileBasedChatMemory(fileDir);
    }

    @Bean("messageWindowChatMemory")
    public MessageWindowChatMemory messageWindowChatMemory() {
        // 初始化基于内存的对话记忆
        return MessageWindowChatMemory.builder()
                                      .chatMemoryRepository(new InMemoryChatMemoryRepository())
                                      .maxMessages(20)
                                      .build();
    }

    @Bean("smartChatMemory")
    public SmartChatMemory smartChatMemory() {
        // 初始化带三层压缩策略的对话记忆
        // 摘要压缩 → Assistant 裁剪 → 滑动窗口兜底
        ChatClient summaryChatClient = ChatClient.builder(dashscopeChatModel).build();
        return new SmartChatMemory(summaryChatClient);
    }

}
