package com.assistant.ai.rpc.domain.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 知识文档新增更新操作
 *
 * @author endcy
 * @date 2025/12/12 17:32:45
 */
@Data
public class KnowledgeDocumentParam implements Serializable {
    private static final long serialVersionUID = -8339257007805947491L;

    /**
     * 知识领域类型
     *
     * @see .KnowledgeScopeTypeEnum
     */
    @NotBlank
    private String scopeType;

    /**
     * 知识业务模块
     *
     * @see .KnowledgeBusinessTypeEnum
     */
    @NotBlank
    private String businessType;

    /**
     * 内容标题
     *
     * @see .KnowledgeContentTypeEnum
     */
    @NotBlank
    private String title;

    /**
     * 内容
     */
    @NotBlank
    private String content;

    /**
     * 内容来源 可选
     *
     * @see .DocSourceTypeEnum
     */
    private String sourceType;

    /**
     * 来源路径 可选
     */
    private String sourcePath;

    /**
     * 文档版本号 可选
     */
    private Integer docVersion;

    /**
     * 是否公开 可选
     */
    private Boolean enablePublic;

    /**
     * 是否可用 可选
     */
    private Boolean enabled;

    /**
     * 过期时间 可选
     */
    private Date expiredTime;

    /**
     * 三方系统文档id
     */
    @NotNull
    private Long id;

    /**
     * 更新文档参数 是否更新文档操作
     */
    private Boolean updateDoc;

}
