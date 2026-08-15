package com.endcy.ai.guardrail;

import lombok.Builder;
import lombok.Data;

/**
 * Guardrail check result.
 *
 * @author endcy
 * @since 2026-08-07
 */
@Data
@Builder
public class GuardrailResult {

    /**
     * Triggered action
     */
    private GuardrailAction action;

    /**
     * Reason for blocking/redaction
     */
    private String reason;

    /**
     * Preset reply when BLOCK
     */
    private String presetResponse;

    /**
     * Content after REDACT
     */
    private String redactedContent;

    // ---- Static factories ----

    public static GuardrailResult pass() {
        return GuardrailResult.builder().action(GuardrailAction.PASS).build();
    }

    public static GuardrailResult block(String reason, String presetResponse) {
        return GuardrailResult.builder()
                              .action(GuardrailAction.BLOCK)
                              .reason(reason)
                              .presetResponse(presetResponse)
                              .build();
    }

    public static GuardrailResult redact(String reason, String redactedContent) {
        return GuardrailResult.builder()
                              .action(GuardrailAction.REDACT)
                              .reason(reason)
                              .redactedContent(redactedContent)
                              .build();
    }

    public boolean isBlocked() {
        return action == GuardrailAction.BLOCK;
    }

    public boolean isRedacted() {
        return action == GuardrailAction.REDACT;
    }
}
