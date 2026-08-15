package com.endcy.ai.repository.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 工具注册实体（对应 ai_tool 表）。
 *
 * <p>启动时由 {@code ToolRegistrySyncService} 自动同步自 ToolRegistry + {@code @McpServer} 注解。</p>
 *
 * @author endcy
 * @since 2026-08-13
 */
@Data
@TableName(value = "ai_tool", autoResultMap = true)
public class Tool implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工具唯一标识（对应 @Tool 方法名或 MCP 工具名）
     */
    private String toolName;

    /**
     * 工具中文名
     */
    private String cnName;

    /**
     * 工具描述（LLM 可见）
     */
    private String description;

    /**
     * 来源：CODE / MCP
     */
    private String source;

    /**
     * MCP 来源时填 server 名（与 @McpServer 注解值对应）
     */
    private String mcpServer;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
