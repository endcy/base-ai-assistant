package com.assistant.ai.rpc.domain.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * ...
 *
 * @author endcy
 * @date 2025/12/12 17:32:45
 */
@Data
public class KnowledgeDocumentMatchParam implements Serializable {
    private static final long serialVersionUID = -8339257007805947491L;

    /**
     * 知识领域类型
     *
     * @see .KnowledgeScopeTypeEnum
     */
    @NotBlank
    private String scopeType;

    /**
     * 知识业务模块 可选
     *
     * @see .KnowledgeBusinessTypeEnum
     */
    private String businessType;

    /**
     * 内容或关键词
     * 多关键词语义匹配,请使用英文逗号分隔关键词
     */
    @NotBlank
    private String question;

    /**
     * 是否公开内容
     */
    private Boolean enablePublic;

    /**
     * 匹配数量 可选
     * 默认匹配量为5
     */
    private Integer similarityTopK;

}
