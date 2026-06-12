package com.assistant.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * QA问答同步返回对象
 *
 * @author endcy
 * @date 2025/12/13 15:54:15
 */
@Data
public class AIAnswerRet implements Serializable {

    private static final long serialVersionUID = 385285639029438753L;

    /**
     * 回答文本
     */
    private String text;

    /**
     * 回答关联文档 可选
     */
    private List<KnowledgeDocumentMatchItem> relatedDocs;

    /**
     * 输入token用量（prompt）
     */
    private int promptTokens;

    /**
     * 输出token用量（completion）
     */
    private int completionTokens;
}
