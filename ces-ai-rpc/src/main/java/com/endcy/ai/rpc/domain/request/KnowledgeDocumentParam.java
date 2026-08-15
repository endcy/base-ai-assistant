package com.endcy.ai.rpc.domain.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 知识文档新增更新操作
 *
 * @author endcy
 * @since 2025/12/12 17:32:45
 */
@Data
public class KnowledgeDocumentParam implements Serializable {
    private static final long serialVersionUID = -8339257007805947491L;

    @NotBlank
    private String scopeType;

    @NotBlank
    private String businessType;

    @NotBlank
    private String title;

    @NotNull
    private String groupId;

    @NotBlank
    private String content;

    private String sourceType;

    private String sourcePath;

    private Integer docVersion;

    private Boolean enablePublic;

    private Boolean enabled;

    private Date expiredTime;

    @NotNull
    private Long docId;

    private Boolean updateDoc;
}
