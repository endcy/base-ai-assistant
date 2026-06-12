package com.assistant.ai.rpc.domain.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * ...
 *
 * @author endcy
 * @date 2025/12/12 17:32:45
 */
@Data
public class KnowledgeDocumentActionParam implements Serializable {
    private static final long serialVersionUID = -8339257007805947491L;

    /**
     * 知识领域类型 可选
     *
     * @see .KnowledgeScopeTypeEnum
     */
    private String scopeType;

    /**
     * 知识业务模块 可选
     *
     * @see .KnowledgeBusinessTypeEnum
     */
    private String businessType;

    /**
     * 刷新文档id载入 可选，与上述scopeType分组查询参数二选一
     */
    private List<Long> ids;

    /**
     * 文档操作类型 0禁用 1启用(自动刷新载入) 2查询状态 3手动刷新载入 4删除
     *
     * @see com.assistant.ai.rpc.enums.DocumentActionType
     */
    @NotNull(message = "操作类型不能为空")
    private Integer action;

}
