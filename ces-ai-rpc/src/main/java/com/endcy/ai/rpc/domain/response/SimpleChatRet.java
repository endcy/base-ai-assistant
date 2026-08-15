package com.endcy.ai.rpc.domain.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 简单问答返回结果
 *
 * @author endcy
 * @since 2026/06/09
 */
@Data
public class SimpleChatRet implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long chatId;

    private Double confidence;

    private Boolean canAnswer;

    private String questionAnswer;
}
