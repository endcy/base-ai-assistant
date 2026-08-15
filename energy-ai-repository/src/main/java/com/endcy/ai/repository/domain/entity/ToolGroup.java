package com.endcy.ai.repository.domain.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工具组实体（对应 ai_tool_group 表）。
 *
 * <p>两种类型：</p>
 * <ul>
 *   <li>MCP 组：自动从 {@code @McpServer} 注解创建，只读，成员按 {@code ai_tool.mcp_server} 自动匹配</li>
 *   <li>CUSTOM 组：用户在页面创建，可编辑，成员走 {@link ToolGroupMember}</li>
 * </ul>
 *
 * @author endcy
 * @since 2026-08-13
 */
@Data
@TableName(value = "ai_tool_group", autoResultMap = true)
public class ToolGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工具组唯一标识
     */
    private String groupName;

    /**
     * 工具组中文名
     */
    private String cnName;

    /**
     * 描述
     */
    private String description;

    /**
     * 类型：MCP / CUSTOM
     */
    private String groupType;

    /**
     * MCP 类型时填 server 名，与 ai_tool.mcp_server 自动匹配成员
     */
    private String mcpServer;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
