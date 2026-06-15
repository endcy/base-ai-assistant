package com.assistant.ai.rpc.domain.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Tolerate;

import java.io.Serializable;

/**
 * 多媒体附件
 * 支持图片、音频、视频、文档等多模态输入
 *
 * @author endcy
 * @date 2026/06/13
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaAttachment implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 媒体类型
     * IMAGE-图片, AUDIO-音频, VIDEO-视频, DOCUMENT-文档
     */
    private String type;

    /**
     * 媒体资源URL（OSS公网URL或带签名的临时URL）
     */
    private String url;

    /**
     * 媒体描述（可选，用于辅助大模型理解）
     */
    private String description;

    /**
     * MIME类型（可选，如image/png, image/jpeg, audio/mpeg等）
     */
    private String mimeType;

    @Tolerate
    public MediaAttachment(String type, String url) {
        this.type = type;
        this.url = url;
    }
}
