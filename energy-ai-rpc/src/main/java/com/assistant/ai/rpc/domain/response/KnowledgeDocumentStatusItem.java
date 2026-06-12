package com.assistant.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * ...
 *
 * @author endcy
 * @date 2025/12/12 17:32:45
 */
@Data
public class KnowledgeDocumentStatusItem implements Serializable {
    private static final long serialVersionUID = -8339257007105947491L;

    /**
     * 三方系统文档id
     */
    private Long id;

    /**
     * 是否已加载至rag(向量库)
     */
    private Boolean loaded;

    /**
     * 是否可用
     */
    private Boolean enabled;

}
