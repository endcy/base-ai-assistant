package com.endcy.ai.rpc.domain.request;

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
 * @since 2026/06/13
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaAttachment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String type;

    private String url;

    private String description;

    private String mimeType;

    @Tolerate
    public MediaAttachment(String type, String url) {
        this.type = type;
        this.url = url;
    }
}
