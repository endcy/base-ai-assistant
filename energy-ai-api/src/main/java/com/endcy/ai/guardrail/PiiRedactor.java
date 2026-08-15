package com.endcy.ai.guardrail;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * PII (Personally Identifiable Information) redaction input guardrail.
 *
 * <p>Step 1.8 enabled by default: detects phone numbers and ID card numbers, performs REDACT then passes through.</p>
 *
 * @author endcy
 * @since 2026-08-07
 */
@Slf4j
@Component
public class PiiRedactor implements InputGuardrail {

    // Phone number: 1[3-9]XXXXXXXXX
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");
    // ID card number: 18 digits (last digit may be X)
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");

    @Override
    public GuardrailResult check(String userInput, List<Message> history) {
        if (StrUtil.isBlank(userInput)) {
            return GuardrailResult.pass();
        }

        String redacted = userInput;
        boolean hasPii = false;

        // Redact phone numbers
        if (PHONE_PATTERN.matcher(redacted).find()) {
            redacted = PHONE_PATTERN.matcher(redacted).replaceAll(m -> {
                String phone = m.group();
                return DesensitizedUtil.mobilePhone(phone);
            });
            hasPii = true;
        }

        // Redact ID card numbers
        if (ID_CARD_PATTERN.matcher(redacted).find()) {
            redacted = ID_CARD_PATTERN.matcher(redacted).replaceAll(m -> {
                String idCard = m.group();
                return DesensitizedUtil.idCardNum(idCard, 6, 2);
            });
            hasPii = true;
        }

        if (hasPii) {
            log.info("PII redaction: sensitive information detected and replaced");
            return GuardrailResult.redact("PII detected, redacted", redacted);
        }

        return GuardrailResult.pass();
    }
}
