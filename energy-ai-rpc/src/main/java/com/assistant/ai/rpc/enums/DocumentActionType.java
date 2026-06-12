package com.assistant.ai.rpc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DocumentActionType {

    /**
     * STREAM数据类型
     */
    DISABLED(0, " 禁用"),
    ENABLED(1, "启用(刷新载入)"),
    STATUS(2, "查看状态"),
    REFRESH(3, "手动刷新载入"),
    DELETE(4, "删除"),
    ;

    private final int code;

    private final String remark;

    public static DocumentActionType getByCode(int code) {
        for (DocumentActionType apiCode : DocumentActionType.values()) {
            if (apiCode.getCode() == code) {
                return apiCode;
            }
        }
        return null;
    }
}
