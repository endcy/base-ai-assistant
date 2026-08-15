package com.endcy.service.domain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an MCP client class, declaring which MCP server it belongs to.
 *
 * <p>Used at startup to automatically identify MCP-originated tools and associate them
 * with the corresponding MCP tool group (record in {@code ai_tool_group} where group_type='MCP').</p>
 *
 * <p>Usage example:</p>
 * <pre>
 * &#64;McpServer("amap")
 * &#64;Component
 * public class AMapMcpClient extends AbstractMcpClient { ... }
 * </pre>
 *
 * @author endcy
 * @since 2026-08-13
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpServer {

    /**
     * MCP server name (unique identifier, corresponds to {@code ai_tool_group.mcp_server}
     * and {@code ai_tool.mcp_server}).
     */
    String value();
}
