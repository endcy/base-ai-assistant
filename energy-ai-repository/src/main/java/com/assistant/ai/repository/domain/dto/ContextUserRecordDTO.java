package com.assistant.ai.repository.domain.dto;

import com.assistant.service.domain.enums.KnowledgeBusinessTypeEnum;
import com.assistant.service.domain.enums.KnowledgeScopeTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 用户对话记录
 *
 * @author endcy
 * @since 2025/08/04 20:51:26
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContextUserRecordDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 121559436960646084L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private Long chatId;

    private Long userId;

    private Integer userType;

    private Long groupId;

    /**
     * 知识领域类型
     *
     * @see KnowledgeScopeTypeEnum
     */
    private String scopeType;

    /**
     * 知识业务模块，意图分类
     *
     * @see KnowledgeBusinessTypeEnum
     */
    private String businessType;

    private String question;

    private String content;

    private Boolean enabled;

    private Date createTime;

    private Date updateTime;

    @Tolerate
    public ContextUserRecordDTO() {
    }

}
