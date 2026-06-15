package com.assistant.ai.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.assistant.ai.repository.domain.dto.ContextUserRecordDTO;
import com.assistant.ai.repository.service.ContextUserRecordService;
import com.assistant.ai.rpc.domain.request.MediaAttachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 从数据库加载历史对话内容
 * 当内存中取不到历史消息时使用
 * 支持多模态历史消息重建（图片、音频、视频等）
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
     * 支持重建多模态消息（UserMessage带Media附件）
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
            // 重建UserMessage（支持多模态）
            UserMessage userMessage = buildUserMessage(record);
            if (userMessage != null) {
                messages.add(userMessage);
            }
            if (StrUtil.isNotBlank(record.getContent())) {
                messages.add(new AssistantMessage(record.getContent()));
            }
        }
        log.info("###### Loaded {} history messages from DB for chatId {}", messages.size(), chatId);
        return messages;
    }

    /**
     * 从数据库记录重建UserMessage
     * 如果record包含mediaInfo，则构建带Media附件的UserMessage
     */
    private UserMessage buildUserMessage(ContextUserRecordDTO record) {
        if (StrUtil.isBlank(record.getQuestion()) && StrUtil.isBlank(record.getMediaInfo())) {
            return null;
        }
        // 解析多媒体附件
        List<Media> mediaList = parseMediaInfo(record.getMediaInfo());
        // 构建UserMessage
        UserMessage.Builder builder = UserMessage.builder();
        if (StrUtil.isNotBlank(record.getQuestion())) {
            builder.text(record.getQuestion());
        }
        if (!mediaList.isEmpty()) {
            builder.media(mediaList.toArray(new Media[0]));
        }
        return builder.build();
    }

    /**
     * 解析mediaInfo JSON为Media对象列表
     */
    private List<Media> parseMediaInfo(String mediaInfoJson) {
        if (StrUtil.isBlank(mediaInfoJson)) {
            return Collections.emptyList();
        }
        try {
            List<MediaAttachment> attachments = JSONUtil.toList(mediaInfoJson, MediaAttachment.class);
            if (CollUtil.isEmpty(attachments)) {
                return Collections.emptyList();
            }
            List<Media> mediaList = new ArrayList<>(attachments.size());
            for (MediaAttachment attachment : attachments) {
                Media media = buildMedia(attachment);
                if (media != null) {
                    mediaList.add(media);
                }
            }
            return mediaList;
        } catch (Exception e) {
            log.warn("Failed to parse mediaInfo JSON: {}", mediaInfoJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * 根据附件信息构建Media对象
     */
    private Media buildMedia(MediaAttachment attachment) {
        if (StrUtil.isBlank(attachment.getUrl())) {
            return null;
        }
        try {
            URI uri = new URI(attachment.getUrl());
            MimeType mimeType = resolveMimeType(attachment);
            return new Media(mimeType, uri);
        } catch (URISyntaxException e) {
            log.warn("Invalid media URL: {}", attachment.getUrl(), e);
            return null;
        }
    }

    /**
     * 根据附件信息解析MimeType
     */
    private static MimeType resolveMimeType(MediaAttachment attachment) {
        if (StrUtil.isNotBlank(attachment.getMimeType())) {
            return MimeType.valueOf(attachment.getMimeType());
        }
        if (attachment.getType() == null) {
            return MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }
        return switch (attachment.getType().toUpperCase()) {
            case "IMAGE" -> MimeTypeUtils.IMAGE_PNG;
            case "AUDIO" -> MimeTypeUtils.parseMimeType("audio/mpeg");
            case "VIDEO" -> MimeTypeUtils.parseMimeType("video/mp4");
            case "DOCUMENT" -> MimeType.valueOf("application/pdf");
            default -> MimeTypeUtils.APPLICATION_OCTET_STREAM;
        };
    }
}
