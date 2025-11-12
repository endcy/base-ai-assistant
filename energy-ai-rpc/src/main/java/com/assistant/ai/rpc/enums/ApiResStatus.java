package com.assistant.ai.rpc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApiResStatus {

    /**
     * 返回状态，可能异常情况需要反馈
     */
    ERROR(-1, "处理异常"),
    /**
     * 正常的业务执行结果
     */
    SUCCESS(0, "业务执行成功"),
    /**
     * 正常处理无异常的业务执行结果 包含某些免处理的判断等
     */
    FAILURE(1, "业务执行失败"),
    ;

    private final int code;

    private final String remark;

    public static ApiResStatus getByCode(int code) {
        for (ApiResStatus apiCode : ApiResStatus.values()) {
            if (apiCode.getCode() == code) {
                return apiCode;
            }
        }
        return FAILURE;
    }
}
