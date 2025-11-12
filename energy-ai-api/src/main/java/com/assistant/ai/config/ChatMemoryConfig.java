package com.assistant.ai.config;

import com.assistant.ai.chatmemory.FileBasedChatMemory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
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
public class ChatMemoryConfig {

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

}
