package com.endcy.ai.mcp.config;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * MCP client per-server authentication configuration.
 *
 * <p>Different MCP servers may use different tokens or even different auth styles
 * (custom token header vs standard Authorization: Bearer), so auth rules are routed by URL prefix:</p>
 *
 * <pre>
 * # base-ai mcp-api (custom token header, three endpoints same process same token)
 * ai.mcp.client.auth.rules[0].url-prefix=http://localhost:8004
 * ai.mcp.client.auth.rules[0].token=base-ai-token
 *
 * # Future integration with other services (standard Bearer, or arbitrary headers)
 * ai.mcp.client.auth.rules[1].url-prefix=https://other-mcp.example.com
 * ai.mcp.client.auth.rules[1].headers.Authorization=Bearer other-token
 * </pre>
 *
 * <p>Matching rule: request URI starts with url-prefix → matches; when multiple match,
 * <b>longest prefix wins</b>, supporting "same host, different paths, different tokens".
 * Falls back to global {@code server.mcp.token} when no rule matches
 * (compat with single-token legacy config).</p>
 *
 * @author endcy
 * @since 2026-08-14
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai.mcp.client.auth")
public class McpClientAuthProperties {

    /**
     * Auth rules list matched by URL prefix.
     */
    private List<AuthRule> rules = new ArrayList<>();

    /**
     * Resolve token for URL (custom token header use).
     * Returns rule's token on match, otherwise fallback (global server.mcp.token), both may be null.
     */
    public String resolveToken(String url, String fallback) {
        AuthRule rule = match(url);
        if (rule != null) {
            return rule.getToken();
        }
        return fallback;
    }

    /**
     * Resolve additional headers for URL (Authorization etc.). Returns empty Map if no match.
     */
    public Map<String, String> resolveHeaders(String url) {
        AuthRule rule = match(url);
        return rule != null ? rule.getHeaders() : Collections.emptyMap();
    }

    /**
     * Longest-prefix match.
     */
    private AuthRule match(String url) {
        if (url == null) {
            return null;
        }
        AuthRule best = null;
        int bestLen = -1;
        for (AuthRule rule : rules) {
            String prefix = rule.getUrlPrefix();
            if (StrUtil.isBlank(prefix)) {
                continue;
            }
            // Ignore trailing slash difference to avoid http://host:8004/ not matching http://host:8004/mcp
            String normalized = StrUtil.removeSuffix(prefix, "/");
            if (url.startsWith(normalized) && normalized.length() > bestLen) {
                best = rule;
                bestLen = normalized.length();
            }
        }
        return best;
    }

    /**
     * Single auth rule.
     */
    @Data
    public static class AuthRule {

        /**
         * URL prefix, e.g. http://localhost:8004 or https://api.example.com:9000/mcp.
         */
        private String urlPrefix;

        /**
         * Inject "token" request header (mcp-api McpServerAuth style).
         */
        private String token;

        /**
         * Additional request headers (arbitrary), e.g. Authorization: Bearer xxx, X-Api-Key: yyy.
         */
        private Map<String, String> headers = new LinkedHashMap<>();
    }
}
