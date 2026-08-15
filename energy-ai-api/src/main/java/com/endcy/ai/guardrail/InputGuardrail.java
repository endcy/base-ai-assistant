package com.endcy.ai.guardrail;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * Input guardrail interface.
 *
 * <p>Step 1.8 skeleton, Step 3.x integrated with {@link com.endcy.ai.agent.executor.DefaultAgentExecutor}.</p>
 *
 * <p>Implementations should include (enabled by configuration):
 * <ul>
 *   <li>{@code PromptInjectionDetector} — rule-based + small model binary classification</li>
 *   <li>{@code JailbreakFilter} — identify templates like "ignore previous instructions"</li>
 *   <li>{@code PiiRedactor} — replace phone/ID/bank card numbers with placeholders</li>
 *   <li>{@code TopicGate} — reject out-of-domain small talk beyond the configured domain</li>
 * </ul>
 *
 * @author endcy
 * @since 2026-08-07
 */
public interface InputGuardrail {

    /**
     * Check user input.
     *
     * @param userInput raw user input
     * @param history   history messages (for context awareness)
     * @return check result (PASS / BLOCK / REDACT)
     */
    GuardrailResult check(String userInput, List<Message> history);
}
