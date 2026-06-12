package com.assistant.ai.rpc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApiQaType {

    /**
     * STREAM数据类型
     */
    RAG(1, "知识库问答"),
    DOMAIN(2, "领域知识问答"),
    DEEPSEEK(3, "DeepSeek在线问答"),
    ;

    private final int code;

    private final String remark;

    public static ApiQaType getByCode(int code) {
        for (ApiQaType apiCode : ApiQaType.values()) {
            if (apiCode.getCode() == code) {
                return apiCode;
            }
        }
        return DOMAIN;
    }
}
