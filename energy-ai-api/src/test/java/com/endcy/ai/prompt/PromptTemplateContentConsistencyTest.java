package com.endcy.ai.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prompt 模板内容一致性测试（Step 1.1 的验证门）。
 *
 * <p>验证 classpath:{@code /prompts/*.st} 文件内容与 {@link PromptTemplateKey#getConstantFallback()}
 * （即 {@code EnergyAiConstant} 内置常量）<b>逐字符一致</b>。</p>
 *
 * <p><b>全部通过后</b>，才能安全地把 {@code ai.prompt.external.enabled} 设为 {@code true}。</p>
 *
 * <p>如果某个 key 失败，测试会报告：文件名、双方长度、第一个差异字符的位置和字符值，
 * 据此修正对应的 {@code .st} 文件（注意空白符、换行、尾随空格）。</p>
 *
 * @author endcy
 * @since 2026-08-07
 */
class PromptTemplateContentConsistencyTest {

    @Test
    void allStFilesMatchConstants() throws IOException {
        StringBuilder failures = new StringBuilder();
        for (PromptTemplateKey key : PromptTemplateKey.values()) {
            String stContent = readSt(key.getFileName());
            String constant = key.getConstantFallback();
            if (!stContent.equals(constant)) {
                reportMismatch(key, stContent, constant, failures);
            }
        }
        assertTrue(failures.length() == 0,
                "Prompt .st 内容与内置常量不一致（共 " + countMismatches(failures) + " 处），需修正 .st 文件：\n" + failures);
    }

    private static int countMismatches(StringBuilder failures) {
        int count = 0;
        for (int i = 0; i < failures.length(); i++) {
            if (failures.charAt(i) == '[') {
                count++;
            }
        }
        return count;
    }

    private static void reportMismatch(PromptTemplateKey key, String st, String constant, StringBuilder out) {
        int diffIdx = firstDiffIndex(st, constant);
        out.append("\n[").append(key.getFileName()).append("] MISMATCH\n");
        out.append("  st.len=").append(st.length()).append(", const.len=").append(constant.length());
        out.append(", firstDiffAt=").append(diffIdx).append("\n");
        out.append("  st   char at diff: ").append(describe(st, diffIdx)).append("\n");
        out.append("  const char at diff: ").append(describe(constant, diffIdx)).append("\n");
        out.append("  st   context: ").append(context(st, diffIdx)).append("\n");
        out.append("  const context: ").append(context(constant, diffIdx)).append("\n");
    }

    private static int firstDiffIndex(String a, String b) {
        int min = Math.min(a.length(), b.length());
        for (int i = 0; i < min; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                return i;
            }
        }
        return min;
    }

    private static String describe(String s, int idx) {
        if (idx >= s.length()) {
            return "<EOF> (字符串已结束)";
        }
        char c = s.charAt(idx);
        return String.format("'%c' (U+%04X, %s)", c, (int) c, charName(c));
    }

    private static String charName(char c) {
        return switch (c) {
            case '\n' -> "换行 LF";
            case '\r' -> "回车 CR";
            case '\t' -> "制表符 TAB";
            case ' ' -> "空格 SPACE";
            default -> "可见字符";
        };
    }

    private static String context(String s, int idx) {
        int start = Math.max(0, idx - 15);
        int end = Math.min(s.length(), idx + 15);
        String snippet = s.substring(start, end).replace("\n", "\\n").replace("\r", "\\r");
        return "..." + snippet + "...";
    }

    private static String readSt(String fileName) throws IOException {
        ClassPathResource res = new ClassPathResource("prompts/" + fileName + ".st");
        try (var is = res.getInputStream()) {
            // 统一换行符为 LF，避免 Windows CRLF 与常量不一致
            return new String(is.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace("\r", "\n");
        }
    }
}
