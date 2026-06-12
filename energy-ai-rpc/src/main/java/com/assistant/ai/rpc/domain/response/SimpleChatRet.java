package com.assistant.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 简单问答返回结果
 *
 * @author endcy
 * @date 2026/06/09
 */
@Data
public class SimpleChatRet implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 对话id
     */
    private Long chatId;

    /**
     * 如存在参考资料，匹配问答置信度（向量匹配度得分）
     */
    private Double confidence;

    /**
     * 如存在参考资料，是否可作为回答（得分是否大于similarityThreshold）
     */
    private Boolean canAnswer;

    /**
     * 参考问题答案
     */
    private String questionAnswer;
}
