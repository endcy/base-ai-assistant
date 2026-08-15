package com.endcy.ai.memory;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Token budget estimator (lightweight, no real tokenizer dependency).
 *
 * <p>Uses heuristic estimation: English 1 word ≈ 1 token, Chinese 1 char ≈ 1.5 tokens.
 * Error within ±20%, sufficient for "preventing context overflow", not suitable for "precise trimming".
 * For precise trimming, replace with Spring AI {@code TokenCountEstimator}.</p>
 *
 * <p><b>Usage</b>:</p>
 * <pre>
 *   TokenCounter counter = new TokenCounter();
 *   for (Message msg : messages) {
 *       counter.count(msg);
 *   }
 *   int estimatedTokens = counter.total();
 * </pre>
 *
 * @author endcy
 * @since 2026-08-07
 */
@Slf4j
@Component
public class TokenCounter {

    /**
     * Default model context budget (reserve for system + tool schemas).
     */
    private static final int DEFAULT_TOTAL_BUDGET = 30_000;
    /**
     * Output reserve
     */
    private static final int OUTPUT_RESERVE = 2_000;
    /**
     * History messages budget ceiling
     */
    private static final int HISTORY_BUDGET = 18_000;

    /**
     * Estimate the token count of a single message.
     */
    public int count(Message message) {
        if (message == null) {
            return 0;
        }
        String content = message.getText() != null ? message.getText() : "";
        return estimate(content);
    }

    /**
     * Estimate the total token count of a message list.
     */
    public int countAll(List<? extends Message> messages) {
        int total = 0;
        for (Message msg : messages) {
            total += count(msg);
        }
        return total;
    }

    /**
     * Trim the longest suffix from the message list that fits within the token budget.
     * Drops old messages from the head, keeps the most recent ones at the tail.
     *
     * @param messages  message list (ordered, oldest first)
     * @param maxTokens maximum token budget
     * @return trimmed message list
     */
    public <T extends Message> List<T> trimToTokenBudget(List<T> messages, int maxTokens) {
        int total = 0;
        int keepStart = messages.size();
        for (int i = messages.size() - 1; i >= 0; i--) {
            int t = count(messages.get(i));
            if (total + t > maxTokens) {
                keepStart = i + 1;
                break;
            }
            total += t;
            keepStart = i;
        }
        if (keepStart == 0) {
            return new java.util.ArrayList<>(messages);
        }
        log.debug("Token budget trim: {} → {} messages ({} → {} est. tokens)",
                messages.size(), messages.size() - keepStart, countAll(messages), total);
        return new java.util.ArrayList<>(messages.subList(keepStart, messages.size()));
    }

    /**
     * Whether the budget is exceeded.
     */
    public boolean exceedsBudget(List<? extends Message> messages, int maxTokens) {
        return countAll(messages) > maxTokens;
    }

    /**
     * Currently available history budget.
     */
    public int getHistoryBudget() {
        return HISTORY_BUDGET;
    }

    /**
     * Total context budget (includes system + output reserve).
     */
    public int getTotalBudget() {
        return DEFAULT_TOTAL_BUDGET;
    }

    // ==================== private ====================

    /**
     * Heuristic token estimation: Chinese 1 char ≈ 1.5 tokens, English 1 word ≈ 1 token.
     */
    static int estimate(String text) {
        if (StrUtil.isBlank(text)) {
            return 0;
        }
        int chineseChars = 0;
        int otherChars = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x4E00 && c <= 0x9FFF) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        // Chinese ~1.5 tokens/char, other ~0.25 tokens/char (4 chars ≈ 1 token)
        return (int) Math.ceil(chineseChars * 1.5 + otherChars * 0.25);
    }
}
