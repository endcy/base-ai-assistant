package com.assistant.ai.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.assistant.ai.repository.domain.dto.ContextUserRecordDTO;
import com.assistant.ai.repository.service.ContextUserRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 从数据库加载历史对话内容
 * 当内存中取不到历史消息时使用
 *
 * @author endcy
 * @date 2026/06/13
 */
@Slf4j
@Service
public class ChatHistoryService {

    private final ContextUserRecordService userRecordService;

    @Value("${ai.chat.history-max-rounds:10}")
    private int historyMaxRounds;

    public ChatHistoryService(ContextUserRecordService userRecordService) {
        this.userRecordService = userRecordService;
    }

    /**
     * 从数据库加载最近N轮对话历史
     *
     * @param chatId 对话ID
     * @return 历史消息列表（UserMessage + AssistantMessage 交替）
     */
    public List<Message> loadHistoryFromDb(Long chatId) {
        List<ContextUserRecordDTO> records = userRecordService.getByChatId(chatId);
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        int totalPairs = records.size();
        int startIdx = Math.max(0, totalPairs - historyMaxRounds);
        List<ContextUserRecordDTO> recentRecords = records.subList(startIdx, totalPairs);
        List<Message> messages = new ArrayList<>(recentRecords.size() * 2);
        for (ContextUserRecordDTO record : recentRecords) {
            if (StrUtil.isNotBlank(record.getQuestion())) {
                messages.add(new UserMessage(record.getQuestion()));
            }
            if (StrUtil.isNotBlank(record.getContent())) {
                messages.add(new AssistantMessage(record.getContent()));
            }
        }
        log.info("###### Loaded {} history messages from DB for chatId {}", messages.size(), chatId);
        return messages;
    }
}
