package com.endcy.ai.repository.domain.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工具组成员实体（对应 ai_tool_group_member 表）。
 *
 * <p>仅 CUSTOM 类型工具组使用；MCP 工具组的成员按 {@code ai_tool.mcp_server} 自动匹配，不走这表。</p>
 *
 * @author endcy
 * @since 2026-08-13
 */
@Data
@TableName(value = "ai_tool_group_member", autoResultMap = true)
public class ToolGroupMember implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工具组 ID
     */
    private Long groupId;

    /**
     * 工具 ID
     */
    private Long toolId;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
