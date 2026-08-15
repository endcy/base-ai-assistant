package com.endcy.ai.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.endcy.ai.config.ChatRagProperties;
import com.endcy.ai.prompt.PromptTemplateKey;
import com.endcy.ai.prompt.PromptTemplateService;
import com.endcy.ai.rpc.domain.request.MediaAttachment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Pre-retrieval media analysis agent.
 * Before RAG retrieval, converts multimedia files (images/audio/video) into text descriptions
 * via a multimodal model, enabling multimedia information to participate in subsequent RAG retrieval
 * and LLM answer generation.
 *
 * <p>Design pattern same as {@link IntentAnalysisAgent}:
 * <ul>
 *   <li>Synchronous blocking call (must complete before RAG retrieval)</li>
 *   <li>Full-chain try-catch graceful degradation (failure returns empty string, does not affect main flow)</li>
 *   <li>Toggle control (ai.rag.enable-media-analysis)</li>
 * </ul>
 *
 * @author endcy
 * @date 2026/08/03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaAnalysisAgent {

    private final ChatClient mediaAnalysisChatClient;
    private final ChatRagProperties chatRagProperties;
    private final PromptTemplateService promptTemplateService;

    private static final String AUDIO_MPEG_MIME = "audio/mpeg";
    private static final String VIDEO_MP4_MIME = "video/mp4";
    private static final String APPLICATION_PDF_MIME = "application/pdf";

    /**
     * Analyze multimedia attachments and return parsed text descriptions.
     * Each attachment is individually analyzed by the multimodal model; results are concatenated with type label prefixes.
     *
     * @param chatId       conversation ID (for logging)
     * @param userQuestion original user question (to guide the multimodal model to focus on relevant info)
     * @param mediaList    list of multimedia attachments
     * @return parsed text description; returns empty string on failure or if no attachments
     */
    public String analyze(Long chatId, String userQuestion, List<MediaAttachment> mediaList) {
        if (CollUtil.isEmpty(mediaList) || !BooleanUtil.isTrue(chatRagProperties.getEnableMediaAnalysis())) {
            return StrUtil.EMPTY;
        }
        try {
            List<String> descriptions = new ArrayList<>();
            for (int i = 0; i < mediaList.size(); i++) {
                MediaAttachment attachment = mediaList.get(i);
                if (StrUtil.isBlank(attachment.getUrl())) {
                    continue;
                }
                String desc = analyzeSingle(attachment, userQuestion);
                if (StrUtil.isNotBlank(desc)) {
                    String typeLabel = resolveTypeLabel(attachment.getType());
                    String prefix = mediaList.size() > 1
                            ? "【" + typeLabel + (i + 1) + "】"
                            : "【" + typeLabel + "】";
                    descriptions.add(prefix + desc);
                }
            }
            String result = String.join("\n", descriptions);
            if (StrUtil.isNotBlank(result)) {
                log.info("chatId: {} 多媒体解析完成, {} 个附件, 描述长度: {}",
                        chatId, mediaList.size(), result.length());
            }
            return result;
        } catch (Exception e) {
            log.error("chatId: {} 多媒体解析失败: {}", chatId, e.getMessage(), e);
            return StrUtil.EMPTY;
        }
    }

    /**
     * Call the multimodal model to analyze a single attachment.
     */
    private String analyzeSingle(MediaAttachment attachment, String userQuestion) {
        try {
            Media media = buildMedia(attachment);
            if (media == null) {
                return StrUtil.EMPTY;
            }
            String userPrompt = String.format(promptTemplateService.getTemplate(PromptTemplateKey.MEDIA_ANALYSIS_USER), userQuestion);

            return mediaAnalysisChatClient.prompt()
                                          .system(promptTemplateService.getTemplate(PromptTemplateKey.MEDIA_ANALYSIS_SYSTEM))
                                          .user(userSpec -> userSpec.text(userPrompt).media(media))
                                          .call()
                                          .content();
        } catch (Exception e) {
            log.warn("附件解析失败 url={}: {}", attachment.getUrl(), e.getMessage());
            // Fallback to preset description when attachment analysis fails
            return StrUtil.blankToDefault(attachment.getDescription(), StrUtil.EMPTY);
        }
    }

    /**
     * Build a Spring AI Media object from attachment info.
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
            log.warn("多媒体URL格式错误: {}", attachment.getUrl());
            return null;
        }
    }

    /**
     * Resolve MimeType from attachment info.
     */
    private MimeType resolveMimeType(MediaAttachment attachment) {
        if (StrUtil.isNotBlank(attachment.getMimeType())) {
            return MimeType.valueOf(attachment.getMimeType());
        }
        if (attachment.getType() == null) {
            return MimeTypeUtils.APPLICATION_OCTET_STREAM;
        }
        return switch (attachment.getType().toUpperCase()) {
            case "IMAGE" -> MimeTypeUtils.IMAGE_PNG;
            case "AUDIO" -> MimeTypeUtils.parseMimeType(AUDIO_MPEG_MIME);
            case "VIDEO" -> MimeTypeUtils.parseMimeType(VIDEO_MP4_MIME);
            case "DOCUMENT" -> MimeType.valueOf(APPLICATION_PDF_MIME);
            default -> MimeTypeUtils.APPLICATION_OCTET_STREAM;
        };
    }

    /**
     * Convert media type to a label string.
     */
    private String resolveTypeLabel(String type) {
        if (StrUtil.isBlank(type)) {
            return "附件";
        }
        return switch (type.toUpperCase()) {
            case "IMAGE" -> "图片";
            case "AUDIO" -> "音频";
            case "VIDEO" -> "视频";
            case "DOCUMENT" -> "文档";
            default -> "附件";
        };
    }

}
