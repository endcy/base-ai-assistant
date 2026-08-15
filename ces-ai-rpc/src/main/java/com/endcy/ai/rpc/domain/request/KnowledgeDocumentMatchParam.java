package com.endcy.ai.rpc.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 知识文档匹配查询参数
 *
 * @author endcy
 * @since 2025/12/12 17:32:45
 */
@Data
public class KnowledgeDocumentMatchParam implements Serializable {
    private static final long serialVersionUID = -8339257007805947491L;

    @NotNull
    private String groupId;

    @NotBlank
    private String scopeType;

    private String businessType;

    @NotBlank
    private String question;

    private Boolean enablePublic;

    private Integer similarityTopK;
}
