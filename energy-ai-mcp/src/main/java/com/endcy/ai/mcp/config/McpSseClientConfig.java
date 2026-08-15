package com.endcy.ai.mcp.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpSseClientProperties;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpStreamableHttpClientProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Auto-wiring configuration for MCP SSE / Streamable HTTP client properties.
 *
 * <p>Performs health checks against configured MCP endpoints and registers only reachable ones.
 * When token auth is enabled, health check requests carry the same auth headers as production requests
 * to avoid 401 being misinterpreted as "unavailable".</p>
 *
 * @author endcy
 * @date 2025/11/5 20:12:06
 * @see McpSseClientProperties
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class McpSseClientConfig {

    private final McpSseConnectionsProperties mcpSseConnectionsProperties;
    private final McpStreamAbleConnectionsProperties mcpStreamAbleConnectionsProperties;
    private final McpClientAuthProperties mcpClientAuthProperties;

    /**
     * Global fallback token (consistent with McpClientAuthConfig, aligns health check auth with production requests).
     */
    @Value("${server.mcp.token:}")
    private String mcpToken;

    @Bean
    @Primary
    public McpSseClientProperties mcpSseClientProperties() {
        McpSseClientProperties mcpSseClientProperties = new McpSseClientProperties();
        Map<String, McpSseClientProperties.SseParameters> connections = mcpSseClientProperties.getConnections();

        Map<String, McpSseClientProperties.SseParameters> existsConnections = mcpSseConnectionsProperties.getConnections();
        if (CollUtil.isEmpty(existsConnections)) {
            return mcpSseClientProperties;
        }
        // Check availability
        existsConnections.forEach((name, sseParameters) -> {
            // Default format convention: url=http://localhost:8004  sse-endpoint=/mcp/sse
            String sseUrl = sseParameters.url() + sseParameters.sseEndpoint();
            try {
                URL url = new URL(sseUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "text/event-stream");
                applyToken(connection);
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    connections.put(name, new McpSseClientProperties.SseParameters(sseParameters.url(), sseParameters.sseEndpoint()));
                }
                connection.disconnect();
            } catch (Exception e) {
                log.error(">>>>>> sse-endpoint: {} get mcp server info error", sseUrl, e);
            }
        });

        return mcpSseClientProperties;
    }


    @Bean
    @Primary
    public McpStreamableHttpClientProperties mcpStreamableHttpClientProperties() {
        McpStreamableHttpClientProperties mcpStreamAbleClientProperties = new McpStreamableHttpClientProperties();
        Map<String, McpStreamableHttpClientProperties.ConnectionParameters> connections = mcpStreamAbleClientProperties.getConnections();

        Map<String, McpStreamableHttpClientProperties.ConnectionParameters> existsConnections = mcpStreamAbleConnectionsProperties.getConnections();
        if (CollUtil.isEmpty(existsConnections)) {
            return mcpStreamAbleClientProperties;
        }
        // Check availability
        existsConnections.forEach((name, parameters) -> {
            // Default format convention: url=http://localhost:8004  sse-endpoint=/mcp/sse
            String streamAbleUrl = parameters.url() + parameters.endpoint();
            try {
                URL url = new URL(streamAbleUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "text/event-stream");
                applyToken(connection);
                int responseCode = connection.getResponseCode();
                // 400 streamable requires token etc., so 400 means server is available but params are wrong
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    connections.put(name, new McpStreamableHttpClientProperties.ConnectionParameters(parameters.url(), parameters.endpoint()));
                }
                connection.disconnect();
            } catch (Exception e) {
                log.error(">>>>>> streamAble-endpoint: {} get mcp server info error", streamAbleUrl, e);
            }
        });

        return mcpStreamAbleClientProperties;
    }

    /**
     * When MCP server token auth is enabled, health check requests must also carry the token header,
     * otherwise 401 will be misinterpreted as "unavailable" and the connection registration will be skipped.
     * Shares the same per-server rule set (longest-prefix wins) as {@link com.endcy.ai.mcp.config.McpClientAuthConfig}
     * for consistent auth.
     */
    private void applyToken(HttpURLConnection connection) {
        String url = connection.getURL().toString();
        String token = mcpClientAuthProperties.resolveToken(url, mcpToken);
        if (StrUtil.isNotBlank(token)) {
            connection.setRequestProperty("token", token);
        }
        mcpClientAuthProperties.resolveHeaders(url)
                               .forEach(connection::setRequestProperty);
    }

}
