package com.endcy.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 文档状态项
 *
 * @author endcy
 * @since 2025/12/12 17:32:45
 */
@Data
public class KnowledgeDocumentStatusItem implements Serializable {
    private static final long serialVersionUID = -8339257007105947491L;

    private Long docId;

    private Boolean loaded;

    private Boolean enabled;
}
