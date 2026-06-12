package com.assistant.ai.rpc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageType {

    /**
     * STREAM数据类型
     */
    DOC(1, "文档"),
    TEXT(2, "文本回答"),
    TOKEN(3, "token计量"),
    ;

    private final int code;

    private final String remark;

    public static MessageType getByCode(int code) {
        for (MessageType apiCode : MessageType.values()) {
            if (apiCode.getCode() == code) {
                return apiCode;
            }
        }
        return TEXT;
    }
}
