package com.endcy.ai.tools.registry;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * Tool parameter validator — Step 2.6.
 *
 * <p>Currently lightweight validation: non-empty, length, basic format.
 * Full JSON Schema validation to be enhanced in Step 2.6.</p>
 *
 * @author endcy
 * @since 2026-08-08
 */
public class ToolValidator {

    private static final Pattern SAFE_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-./\\\\:]+$");
    private static final int MAX_STRING_LENGTH = 10_000;

    /**
     * Validate tool input parameters (simplified: check empty values and overly long strings).
     *
     * @return null if valid, otherwise error message
     */
    public static String validate(String toolName, String argumentsJson) {
        if (StrUtil.isBlank(argumentsJson)) {
            // Empty arguments may be valid (for parameterless tools), do not error
            return null;
        }
        if (argumentsJson.length() > MAX_STRING_LENGTH) {
            return "Tool arguments too long (>" + MAX_STRING_LENGTH + " chars), suspected injection attack";
        }
        // Simple dangerous pattern detection
        String lower = argumentsJson.toLowerCase();
        if (lower.contains("<script") || lower.contains("javascript:")) {
            return "Tool arguments contain suspicious script content";
        }
        return null;
    }

    /**
     * Validate file path parameter, prevent directory traversal.
     */
    public static String validateFilePath(String path) {
        if (StrUtil.isBlank(path)) {
            return "Path is empty";
        }
        if (path.contains("..")) {
            return "Path contains '..', suspected directory traversal attack";
        }
        return null;
    }

    /**
     * Validate URL parameter, restrict to http/https.
     */
    public static String validateUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return "URL is empty";
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "URL must start with http:// or https://";
        }
        return null;
    }

    private ToolValidator() {
        // Utility class
    }
}
