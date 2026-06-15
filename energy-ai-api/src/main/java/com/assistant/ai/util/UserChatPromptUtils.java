package com.assistant.ai.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.assistant.ai.rpc.domain.request.KnowledgeAIQueryParam;
import com.assistant.ai.rpc.domain.request.MediaAttachment;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 用户提示词工具类
 * 支持纯文本和多模态（图片、音频、视频等）输入
 *
 * @author endcy
 * @date 2026/6/13
 */
public class UserChatPromptUtils {

    /**
     * 构造ChatClient的user spec消费者
     * 支持纯文本和多模态附件
     *
     * @param query 用户查询参数
     * @return Consumer<ChatClient.PromptUserSpec>
     */
    @NotNull
    public static Consumer<ChatClient.PromptUserSpec> generatePromptUserSpecConsumer(KnowledgeAIQueryParam query) {
        return userSpec -> {
            // 纯文本内容
            if (StrUtil.isNotBlank(query.getQuestion())) {
                userSpec.text(query.getQuestion());
            }
            // 多模态附件
            List<MediaAttachment> mediaList = query.getMediaList();
            if (CollUtil.isNotEmpty(mediaList)) {
                List<Media> mediaObjects = new ArrayList<>(mediaList.size());
                for (MediaAttachment attachment : mediaList) {
                    Media media = buildMedia(attachment);
                    if (media != null) {
                        mediaObjects.add(media);
                    }
                }
                if (!mediaObjects.isEmpty()) {
                    userSpec.media(mediaObjects.toArray(new Media[0]));
                }
            }
        };
    }

    /**
     * 根据附件信息构建Media对象
     */
    private static Media buildMedia(MediaAttachment attachment) {
        if (StrUtil.isBlank(attachment.getUrl())) {
            return null;
        }
        try {
            URI uri = new URI(attachment.getUrl());
            MimeType mimeType = resolveMimeType(attachment);
            return new Media(mimeType, uri);
        } catch (URISyntaxException e) {
            throw new RuntimeException("多媒体URL格式错误: " + attachment.getUrl(), e);
        }
    }

    /**
     * 根据附件信息解析MimeType
     * 优先使用指定的mimeType，否则根据type字段推断
     */
    private static MimeType resolveMimeType(MediaAttachment attachment) {
        // 优先使用显式指定的mimeType
        if (StrUtil.isNotBlank(attachment.getMimeType())) {
            return MimeType.valueOf(attachment.getMimeType());
        }
        // 根据type字段推断
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
