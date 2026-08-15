package com.endcy.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 查询文档状态返回
 *
 * @author endcy
 * @since 2025/12/12 17:32:45
 */
@Data
public class KnowledgeDocumentStatusRet implements Serializable {
    private static final long serialVersionUID = -8339257007105947491L;

    private List<KnowledgeDocumentStatusItem> docStatusList;
}
