package com.assistant.ai.mcp.config;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpSseClientProperties;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpStreamableHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * 解决自动注入
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

    @Bean
    @Primary
    public McpSseClientProperties mcpSseClientProperties() {
        McpSseClientProperties mcpSseClientProperties = new McpSseClientProperties();
        Map<String, McpSseClientProperties.SseParameters> connections = mcpSseClientProperties.getConnections();

        Map<String, McpSseClientProperties.SseParameters> existsConnections = mcpSseConnectionsProperties.getConnections();
        if (CollUtil.isEmpty(existsConnections)) {
            return mcpSseClientProperties;
        }
        //检查是否可用
        existsConnections.forEach((name, sseParameters) -> {
            //约定使用默认配置格式 例: url=http://localhost:8004  sse-endpoint=/mcp/sse
            String sseUrl = sseParameters.url() + sseParameters.sseEndpoint();
            try {
                //测试请求是否正常
                URL url = new URL(sseUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "text/event-stream");
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    connections.put(name, new McpSseClientProperties.SseParameters(sseParameters.url(), sseParameters.sseEndpoint()));
                }
                connection.disconnect();
            } catch (Exception e) {
                log.error(">>>>>> sse-endpoint: " + sseUrl + " get mcp server info error", e.getMessage());
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
        //检查是否可用
        existsConnections.forEach((name, parameters) -> {
            //约定使用默认配置格式 例: url=http://localhost:8004  sse-endpoint=/mcp/sse
            String streamAbleUrl = parameters.url() + parameters.endpoint();
            try {
                //测试请求是否正常
                URL url = new URL(streamAbleUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "text/event-stream");
                int responseCode = connection.getResponseCode();
                //400 streamable 需要token之类的 所以返回400表示服务器可用 但参数不对
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                    connections.put(name, new McpStreamableHttpClientProperties.ConnectionParameters(parameters.url(), parameters.endpoint()));
                }
                connection.disconnect();
            } catch (Exception e) {
                log.error(">>>>>> streamAble-endpoint: " + streamAbleUrl + " get mcp server info error", e.getMessage());
            }
        });

        return mcpStreamAbleClientProperties;
    }


}
