package com.endcy.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * QA问答同步返回对象
 *
 * @author endcy
 * @since 2025/12/13 15:54:15
 */
@Data
public class AIAnswerRet implements Serializable {

    private static final long serialVersionUID = 385285639029438753L;

    private String text;

    private List<KnowledgeDocumentMatchItem> relatedDocs;

    private int promptTokens;

    private int completionTokens;
}
