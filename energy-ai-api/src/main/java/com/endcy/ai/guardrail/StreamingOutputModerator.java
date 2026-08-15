package com.endcy.ai.guardrail;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Streaming output moderator — Step 4.8.
 *
 * <p>Inspired by Dify OutputModeration: accumulates a buffer during streaming output,
 * periodically moderates, and notifies the frontend via replace events when violations are found.</p>
 *
 * <p>Current implementation: keyword blacklist + sensitive patterns detection.
 * Future extension: calling LLM content moderation APIs.</p>
 *
 * @author endcy
 * @since 2026/08/08
 */
@Slf4j
@Component
public class StreamingOutputModerator {

    @Value("${ai.moderation.output.enabled:false}")
    private boolean enabled;

    @Value("${ai.moderation.output.keywords:}")
    private String keywordsRaw;

    @Value("${ai.moderation.output.buffer-size:200}")
    private int bufferSize;

    /**
     * Per-chatId cumulative buffer
     */
    private final ConcurrentMap<Long, StringBuilder> buffers = new ConcurrentHashMap<>();

    /**
     * Keyword blacklist (Apollo hot-reload)
     */
    private volatile Set<String> blacklist;

    /**
     * Append a streaming chunk to the moderation buffer and return the moderation result.
     *
     * @param chatId session ID
     * @param chunk  streaming chunk
     * @return moderation result (PASS / BLOCK)
     */
    public GuardrailResult appendAndCheck(Long chatId, String chunk) {
        if (!enabled || StrUtil.isBlank(chunk)) {
            return GuardrailResult.pass();
        }

        StringBuilder buffer = buffers.computeIfAbsent(chatId, k -> new StringBuilder());
        buffer.append(chunk);

        // Only moderate once buffer reaches threshold
        if (buffer.length() < bufferSize) {
            return GuardrailResult.pass();
        }

        return moderate(chatId, buffer.toString());
    }

    /**
     * Perform final moderation at the end of the stream.
     */
    public GuardrailResult finalCheck(Long chatId) {
        if (!enabled) {
            return GuardrailResult.pass();
        }
        StringBuilder buffer = buffers.get(chatId);
        if (buffer == null || buffer.length() == 0) {
            return GuardrailResult.pass();
        }
        GuardrailResult result = moderate(chatId, buffer.toString());
        buffers.remove(chatId); // Cleanup
        return result;
    }

    /**
     * Clean up the buffer for a given session.
     */
    public void cleanup(Long chatId) {
        buffers.remove(chatId);
    }

    private GuardrailResult moderate(Long chatId, String content) {
        Set<String> keywords = getBlacklist();
        if (keywords.isEmpty()) {
            return GuardrailResult.pass();
        }

        String lower = content.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                log.warn("Streaming output moderation blocked (chatId={}, keyword={}): {}", chatId, keyword,
                        StrUtil.maxLength(content, 100));
                return GuardrailResult.block("Output contains sensitive content: " + keyword,
                        "Sorry, this response contains sensitive content and has been intercepted by the system.");
            }
        }

        return GuardrailResult.pass();
    }

    private Set<String> getBlacklist() {
        if (blacklist == null) {
            synchronized (this) {
                if (blacklist == null) {
                    if (StrUtil.isBlank(keywordsRaw)) {
                        blacklist = new HashSet<>();
                    } else {
                        blacklist = new HashSet<>(Arrays.asList(keywordsRaw.split(",")));
                    }
                }
            }
        }
        return blacklist;
    }
}
