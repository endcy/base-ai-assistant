package com.endcy.ai.repository.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 领域类型-工具类别权限配置实体
 *
 * @author endcy
 * @since 2026-08-13
 */
@Data
@TableName(value = "ai_scope_tool_config", autoResultMap = true)
public class ScopeToolConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 领域类型（对应 KnowledgeScopeTypeEnum）
     */
    private String scopeType;

    /**
     * 工具组名（关联 ai_tool_group.group_name）
     */
    private String toolGroup;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
