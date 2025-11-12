package com.assistant.ai.repository.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import org.springframework.data.annotation.Id;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户对话记录表
 *
 * @author endcy
 * @since 2025/08/04 20:51:26
 */
@Data
@TableName(value = "ai_context_user_record", autoResultMap = true)
public class ContextUserRecord implements Serializable {

    @Serial
    private static final long serialVersionUID = 129559436960646084L;

    @Id
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long chatId;

    private Long userId;

    private Integer userType;

    private Long groupId;

    private String scopeType;

    private String businessType;

    private String question;

    private String content;

    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

}
