package com.assistant.ai.mcp.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpSseClientProperties;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpStreamableHttpClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @see McpSseClientProperties
 *
 * @author endcy
 * @date 2025/10/27
 */
@Getter
@Data
@Component
@ConfigurationProperties(McpStreamAbleConnectionsProperties.CONFIG_PREFIX)
public class McpStreamAbleConnectionsProperties {

    public static final String CONFIG_PREFIX = "spring.ai.mcp.client.streamable-http.fix";

    private final Map<String, McpStreamableHttpClientProperties.ConnectionParameters> connections = new HashMap<>();

}
