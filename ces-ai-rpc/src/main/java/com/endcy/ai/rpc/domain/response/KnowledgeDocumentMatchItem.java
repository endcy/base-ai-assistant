package com.endcy.ai.rpc.domain.response;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.io.Serializable;

/**
 * 文档匹配项
 *
 * @author endcy
 * @since 2025/12/12 17:32:45
 */
@Data
@Builder
public class KnowledgeDocumentMatchItem implements Serializable {
    private static final long serialVersionUID = -8331257007105947091L;

    @Tolerate
    public KnowledgeDocumentMatchItem() {
    }

    private Long docId;

    private String scopeType;

    private String businessType;

    private String title;

    private String source;

    private Double score;
}
