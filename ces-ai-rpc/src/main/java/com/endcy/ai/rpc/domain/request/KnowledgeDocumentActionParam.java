package com.endcy.ai.rpc.domain.request;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 知识文档操作参数
 *
 * @author endcy
 * @since 2025/12/12 17:32:45
 */
@Data
public class KnowledgeDocumentActionParam implements Serializable {
    private static final long serialVersionUID = -8339257007805947491L;

    @NotNull
    private String groupId;

    private String scopeType;

    private String businessType;

    private List<Long> docIds;

    private List<KnowledgeDocumentParam> publishDocs;

    /**
     * 文档操作类型 0禁用 1启用 2查询状态 3手动刷新载入 4删除
     *
     * @see com.endcy.ai.rpc.enums.DocumentActionType
     */
    @NotNull(message = "操作类型不能为空")
    private Integer action;
}
