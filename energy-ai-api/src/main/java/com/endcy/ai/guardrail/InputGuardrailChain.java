package com.endcy.ai.guardrail;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Input guardrail chain orchestrator — chains all enabled {@link InputGuardrail} instances.
 *
 * <p>Order: PII redaction → (future extensions: injection detection → jailbreak detection → topic filtering).</p>
 *
 * @author endcy
 * @since 2026-08-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InputGuardrailChain implements InputGuardrail {

    private final PiiRedactor piiRedactor;

    @Override
    public GuardrailResult check(String userInput, List<Message> history) {
        // Execute sequentially, short-circuit on any BLOCK
        List<InputGuardrail> chain = CollUtil.newArrayList(
                piiRedactor
                // TODO Step 3.x: Add PromptInjectionDetector / JailbreakFilter / TopicGate
        );

        String current = userInput;
        for (InputGuardrail guardrail : chain) {
            GuardrailResult result = guardrail.check(current, history);
            if (result.isBlocked()) {
                return result;
            }
            if (result.isRedacted()) {
                current = result.getRedactedContent();
            }
        }

        // If any guardrail in the chain performed REDACT, return the final REDACT result
        if (!current.equals(userInput)) {
            return GuardrailResult.redact("After guardrail chain redaction", current);
        }
        return GuardrailResult.pass();
    }
}
