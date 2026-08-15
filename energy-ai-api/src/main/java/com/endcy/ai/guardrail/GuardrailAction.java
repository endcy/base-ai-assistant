package com.endcy.ai.guardrail;

/**
 * Guardrail action semantics (inspired by Dify Moderation design).
 *
 * @author endcy
 * @since 2026-08-07
 */
public enum GuardrailAction {

    /**
     * Block directly, return presetResponse
     */
    BLOCK,

    /**
     * Soft rewrite (e.g. PII redaction), continue the flow
     */
    REDACT,

    /**
     * Allow through
     */
    PASS,
}
