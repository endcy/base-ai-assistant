package com.endcy.ai.rpc.domain.request;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * 文档状态查询参数
 *
 * @author endcy
 * @since 2025/12/12 17:32:45
 */
@Data
public class KnowledgeDocumentStatusParam implements Serializable {
    private static final long serialVersionUID = -8139257007805947491L;

    @NotEmpty
    private List<Long> docIds;
}
