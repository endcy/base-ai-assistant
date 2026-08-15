package com.endcy.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * RAG文档召回匹配测试返回结果
 *
 * @author endcy
 * @since 2026/05/19
 */
@Data
public class RagDocumentMatchRet implements Serializable {

    private static final long serialVersionUID = 1L;

    private Double confidence;

    private Boolean canAnswer;

    private String recommendedQuestions;

    private String questionAnswer;
}
