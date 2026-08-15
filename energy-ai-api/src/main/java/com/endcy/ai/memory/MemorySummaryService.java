package com.endcy.ai.memory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Conversation history summary service — compresses old conversation turns into a summary
 * to avoid context overflow.
 *
 * <p>When the history exceeds {@code ai.chat.summary.threshold-rounds} rounds, the earliest
 * rounds are compressed by the LLM into a summary, keeping the most recent N rounds intact.</p>
 *
 * <p><b>Usage</b>:</p>
 * <pre>
 *   List&lt;Message&gt; all = chatHistoryService.loadHistoryFromDb(chatId);
 *   List&lt;Message&gt; compressed = memorySummaryService.compressIfNeeded(all, chatId);
 *   // compressed = [summary (if triggered)] + [most recent N rounds intact]
 * </pre>
 *
 * <p><b>Degradation</b>: on summary failure, returns the original history (no information loss), only logs.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemorySummaryService {

    private static final String SUMMARY_SYSTEM_PROMPT = """
            你是对话历史压缩助手。请把以下多轮对话压缩成一段不超过 300 字的摘要，
            保留：用户身份信息、关键业务实体（站点号/订单号/设备号）、用户偏好、未解决的问题。
            不要编造对话中没有的信息。直接输出摘要文本，不要任何前缀。
            """;

    private final DashScopeChatModel dashscopeChatModel;
    private final TokenCounter tokenCounter;

    /**
     * Round threshold that triggers summarization (only compress when exceeded).
     */
    @Value("${ai.chat.summary.threshold-rounds:8}")
    private int thresholdRounds;

    /**
     * Most recent complete rounds to keep after summarization.
     */
    @Value("${ai.chat.summary.keep-recent-rounds:4}")
    private int keepRecentRounds;

    /**
     * If the history is too long, compress the earliest portion.
     *
     * @param history complete history (oldest first)
     * @param chatId  session ID (for logging)
     * @return compressed message list (may contain 1 summary + most recent N rounds)
     */
    public List<Message> compressIfNeeded(List<Message> history, Long chatId) {
        if (CollUtil.isEmpty(history) || history.size() <= thresholdRounds * 2) {
            return history; // not long enough, return as-is
        }

        // Split: first half to compress, second half to keep
        int keepCount = keepRecentRounds * 2;
        int splitIdx = Math.max(0, history.size() - keepCount);
        List<Message> toCompress = history.subList(0, splitIdx);
        List<Message> toKeep = history.subList(splitIdx, history.size());

        try {
            String summary = summarize(toCompress);
            if (StrUtil.isBlank(summary)) {
                log.warn("摘要为空，保留原始历史 (chatId={})", chatId);
                return history;
            }
            // Summary injected as a system message at the head
            Message summaryMsg = new org.springframework.ai.chat.messages.SystemMessage(
                    "【历史对话摘要】\n" + summary);
            List<Message> result = new java.util.ArrayList<>();
            result.add(summaryMsg);
            result.addAll(toKeep);
            log.info("历史压缩: {} 条 → 1 条摘要 + {} 条完整 (chatId={})",
                    toCompress.size(), toKeep.size(), chatId);
            return result;
        } catch (Exception e) {
            log.warn("摘要失败，保留原始历史 (chatId={}): {}", chatId, e.getMessage());
            return history;
        }
    }

    /**
     * Call the LLM to compress a segment of history.
     */
    private String summarize(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message m : messages) {
            sb.append(m.getMessageType()).append(": ")
              .append(StrUtil.maxLength(m.getText(), 500))
              .append("\n");
        }
        String userPrompt = "请压缩以下对话历史：\n\n" + sb;

        return ChatClient.builder(dashscopeChatModel)
                         .build()
                         .prompt()
                         .system(SUMMARY_SYSTEM_PROMPT)
                         .user(userPrompt)
                         .call()
                         .content();
    }

    /**
     * Calculate the expected token savings after compression (for deciding whether it is worth compressing).
     */
    public int estimateSavings(List<Message> history) {
        if (CollUtil.isEmpty(history) || history.size() <= thresholdRounds * 2) {
            return 0;
        }
        int keepCount = keepRecentRounds * 2;
        int splitIdx = Math.max(0, history.size() - keepCount);
        int originalTokens = tokenCounter.countAll(history);
        int compressedTokens = 300 + tokenCounter.countAll(history.subList(splitIdx, history.size()));
        return Math.max(0, originalTokens - compressedTokens);
    }
}
