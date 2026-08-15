package com.endcy.ai.manager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.endcy.ai.config.ChatRagProperties;
import com.endcy.ai.repository.domain.dto.ContextUserRecordDTO;
import com.endcy.ai.repository.service.ContextUserRecordService;
import com.endcy.ai.rpc.domain.request.MediaAttachment;
import lombok.RequiredArgsConstructor;
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
import java.util.Comparator;
import java.util.List;

/**
 * 从数据库加载历史对话内容。
 * 支持多模态历史消息重建（图片、音频、视频等）。
 *
 * @author endcy
 * @since 2026/06/13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ContextUserRecordService userRecordService;
    private final ChatRagProperties chatRagProperties;

    @Value("${ai.chat.history-max-rounds:10}")
    private int historyMaxRounds;

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

    /**
     * 加载最近的 N 轮"已完成"对话历史。
     */
    public List<Message> loadHistoryFromDb(Long chatId) {
        List<ContextUserRecordDTO> allRecords = userRecordService.getByChatId(chatId);
        int maxRecords = chatRagProperties.getMaxContextRecords() != null ? chatRagProperties.getMaxContextRecords() : allRecords.size();
        List<ContextUserRecordDTO> records = maxRecords > 0 && allRecords.size() > maxRecords
                ? allRecords.subList(allRecords.size() - maxRecords, allRecords.size())
                : allRecords;
        if (CollUtil.isEmpty(records)) {
            return Collections.emptyList();
        }
        List<ContextUserRecordDTO> completedRecords = records.stream()
                                                             .filter(r -> StrUtil.isNotBlank(r.getContent()))
                                                             .sorted(Comparator.comparing(ContextUserRecordDTO::getId))
                                                             .toList();
        if (completedRecords.isEmpty()) {
            return Collections.emptyList();
        }
        int totalPairs = completedRecords.size();
        int startIdx = Math.max(0, totalPairs - historyMaxRounds);
        List<ContextUserRecordDTO> recentRecords = completedRecords.subList(startIdx, totalPairs);
        List<Message> messages = new ArrayList<>(recentRecords.size() * 2);
        for (ContextUserRecordDTO record : recentRecords) {
            UserMessage userMessage = buildUserMessage(record);
            if (userMessage != null) {
                messages.add(userMessage);
            }
            messages.add(new AssistantMessage(record.getContent()));
        }
        log.info("###### Loaded {} history messages from DB for chatId {}", messages.size(), chatId);
        return messages;
    }

    private UserMessage buildUserMessage(ContextUserRecordDTO record) {
        if (StrUtil.isBlank(record.getQuestion()) && StrUtil.isBlank(record.getMediaInfo())) {
            return null;
        }
        List<Media> mediaList = parseMediaInfo(record.getMediaInfo());
        UserMessage.Builder builder = UserMessage.builder();
        if (StrUtil.isNotBlank(record.getQuestion())) {
            builder.text(record.getQuestion());
        }
        if (!mediaList.isEmpty()) {
            builder.media(mediaList.toArray(new Media[0]));
        }
        return builder.build();
    }

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
}
