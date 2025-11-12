package com.assistant.ai.repository.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.assistant.service.domain.enums.DocSourceTypeEnum;
import com.assistant.service.domain.enums.KnowledgeBusinessTypeEnum;
import com.assistant.service.domain.enums.KnowledgeContentTypeEnum;
import com.assistant.service.domain.enums.KnowledgeScopeTypeEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识库文档
 *
 * @author endcy
 * @since 2025/08/04 20:51:26
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KnowledgeDocumentDTO implements Serializable {

    public static final String[] RAG_META_IGNORE_FIELDS = new String[]{"docVersion", "content", "loaded", "enabled",
//            "enablePublic", "expiredTime",
            "createUser", "updateUser", "createTime", "updateTime"};

    @Serial
    private static final long serialVersionUID = 2383263090210064149L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 知识领域类型
     *
     * @see KnowledgeScopeTypeEnum
     */
    @NotBlank
    private String scopeType;

    /**
     * 知识业务模块
     *
     * @see KnowledgeBusinessTypeEnum
     */
    @NotBlank
    private String businessType;

    /**
     * 内容标题
     *
     * @see KnowledgeContentTypeEnum
     */
    @NotBlank
    private String title;

    /**
     * 内容分组id，如租户id
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;

    /**
     * 内容
     */
    @NotBlank
    private String content;

    /**
     * 内容来源
     *
     * @see DocSourceTypeEnum
     */
    private String sourceType;

    /**
     * 来源路径
     */
    private String sourcePath;

    /**
     * 文档版本号
     */
    private Integer docVersion;

    /**
     * 是否公开
     */
    private Boolean enablePublic;

    /**
     * 是否已加载至rag(向量库)
     */
    private Boolean loaded;

    /**
     * 是否可用
     */
    private Boolean enabled;

    /**
     * 过期时间
     */
    private Date expiredTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createUser;

    private Date createTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateUser;

    private Date updateTime;

}
