package com.assistant.ai.rpc.domain.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 查询文档匹配列表
 * 结构预留，便于拓展其他属性
 *
 * @author endcy
 * @date 2025/12/12 17:32:45
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class KnowledgeDocumentMatchRet implements Serializable {
    private static final long serialVersionUID = -8339057007105947491L;

    /**
     * AI知识库文档
     */
    private List<KnowledgeDocumentMatchItem> docMatchList;

}
