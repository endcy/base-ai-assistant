package com.assistant.ai.repository.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * db中的知识库文档
 * 属性数据类型必须简单，方便元数据检索
 *
 * @author endcy
 * @since 2025/08/04 20:51:26
 */
@Data
@TableName(value = "ai_knowledge_document", autoResultMap = true)
public class KnowledgeDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 699559436960646084L;

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String scopeType;

    private String businessType;

    private String title;

    private Long groupId;

    private String content;

    private String sourceType;

    private String sourcePath;

    private Integer docVersion;

    private Boolean enablePublic;

    private Boolean loaded;

    private Boolean enabled;

    private Date expiredTime;

    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private Long updateUser;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

}
