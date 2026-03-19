package com.assistant.ai.mcp.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.ai.mcp.client.common.autoconfigure.properties.McpSseClientProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author endcy
 * @date 2025/10/27
 * @see McpSseClientProperties
 */
@Getter
@Data
@Component
@ConfigurationProperties(McpSseConnectionsProperties.CONFIG_PREFIX)
public class McpSseConnectionsProperties {

    public static final String CONFIG_PREFIX = "spring.ai.mcp.client.sse.fix";

    private final Map<String, McpSseClientProperties.SseParameters> connections = new HashMap<>();

}
