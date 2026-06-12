package com.assistant.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * RAG文档召回匹配测试返回结果
 *
 * @author endcy
 * @date 2026/05/19
 */
@Data
public class RagDocumentMatchRet implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 置信度（向量匹配度得分）
     */
    private Double confidence;

    /**
     * 是否可作为回答（得分是否大于similarityThreshold）
     */
    private Boolean canAnswer;

    /**
     * 推荐问题列表（当canAnswer为false时，由AI推荐的相关问题）
     */
    private String recommendedQuestions;

    /**
     * 参考问题答案
     */
    private String questionAnswer;
}
