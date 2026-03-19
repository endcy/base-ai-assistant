package com.assistant.ai.repository.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识分类配置实体
 *
 * @author endcy
 * @since 2026/03/18
 */
@Data
@TableName(value = "ai_knowledge_category_config", autoResultMap = true)
public class KnowledgeCategoryConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分类类型 (scope-知识领域，business-业务领域)
     */
    private String type;

    /**
     * 分类编码 (英文标识)
     */
    private String code;

    /**
     * 分类名称 (中文显示)
     */
    private String name;

    /**
     * 父级分类编码 (业务类型可选填)
     */
    private String parentCode;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序序号
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 创建人
     */
    private Long createUser;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新人
     */
    private Long updateUser;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

}
