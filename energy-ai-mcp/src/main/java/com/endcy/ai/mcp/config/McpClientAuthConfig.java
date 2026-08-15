package com.endcy.ai.mcp.config;

import cn.hutool.core.util.StrUtil;
import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * MCP client authentication — per-server auth rules, coordinates with {@code McpServerAuth}
 * filters and other service authentication.
 *
 * <p>Resolution order (each HTTP request evaluated independently by request URI):</p>
 * <ol>
 *   <li>{@code ai.mcp.client.auth.rules[n].url-prefix} matches → inject that rule's
 *       {@code token} header (mcp-api style) + any additional headers (Bearer / X-Api-Key etc.)</li>
 *   <li>No match but global {@code server.mcp.token} configured → inject for all requests (single-token compat mode)</li>
 *   <li>Neither → no injection, no auth</li>
 * </ol>
 *
 * <p>Spring AI 1.1.7's {@code StreamableHttpHttpClientTransportAutoConfiguration}
 * auto-collects the unique {@code McpSyncHttpClientRequestCustomizer} bean via {@code ObjectProvider.ifUnique()}
 * and applies it to each connection's transport.
 * Note: only one bean of this type; multiple will cause ifUnique to not trigger.</p>
 *
 * @author endcy
 * @since 2026-08-14
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class McpClientAuthConfig {

    private final McpClientAuthProperties authProperties;

    /**
     * Global fallback token (simplified config when all MCP servers share the same token).
     * Overridden by per-server rules when they match.
     */
    @Value("${server.mcp.token:}")
    private String globalToken;

    /**
     * Per-MCP-HTTP-request auth injection based on target URL.
     */
    @Bean
    public McpSyncHttpClientRequestCustomizer mcpTokenCustomizer() {
        log.info("[MCP Auth] Auth init: per-server rules {}, global token {}",
                authProperties.getRules().size(),
                StrUtil.isNotBlank(globalToken) ? "configured" : "not configured");
        return (requestBuilder, method, uri, body, transportContext) ->
                applyAuth(requestBuilder, uri.toString());
    }

    /**
     * Inject auth header per request URL (longest-prefix wins, see {@link McpClientAuthProperties}).
     */
    void applyAuth(java.net.http.HttpRequest.Builder requestBuilder, String url) {
        String token = authProperties.resolveToken(url, globalToken);
        if (StrUtil.isNotBlank(token)) {
            requestBuilder.header("token", token);
        }
        Map<String, String> headers = authProperties.resolveHeaders(url);
        headers.forEach(requestBuilder::header);
    }

}
