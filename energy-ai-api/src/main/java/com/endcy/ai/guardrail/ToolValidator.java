package com.endcy.ai.guardrail;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Tool parameter validator — validates the legality of tool parameters.
 *
 * @author endcy
 * @since 2026/08/08
 */
@Slf4j
public class ToolValidator {

    private static final int MAX_TOOL_INPUT_LENGTH = 10000;

    /**
     * Validate tool parameters.
     *
     * @param toolName  tool name
     * @param toolInput tool input (JSON string)
     * @return error message (null indicates pass)
     */
    public static String validate(String toolName, String toolInput) {
        if (StrUtil.isBlank(toolInput)) {
            return null; // Allow empty input
        }

        // 1. Length check
        if (toolInput.length() > MAX_TOOL_INPUT_LENGTH) {
            return "参数过长（>" + MAX_TOOL_INPUT_LENGTH + " 字符）";
        }

        // 2. Injection attack detection
        String lower = toolInput.toLowerCase();
        if (lower.contains("<script") || lower.contains("javascript:") || lower.contains("eval(")) {
            return "检测到可疑的脚本内容";
        }

        // 3. SQL injection detection (simple detection)
        if (lower.contains("drop table") || lower.contains("delete from") || lower.contains("truncate")) {
            return "检测到可疑的 SQL 语句";
        }

        // 4. File path security check
        if (toolInput.contains("../") || toolInput.contains("..\\")) {
            return "检测到目录穿越攻击";
        }

        return null; // Pass
    }
}
