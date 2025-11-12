package com.assistant.ai.rpc.domain.base;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 基类 设备请求消息规约
 * T 指令参数类型类
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommonReqMsgDTO<T> extends BaseReqMsgDTO {
    private static final long serialVersionUID = 7555168977714964177L;

    /**
     * 字符类型 版本号
     */
    private String ver;

    /**
     * 命令参数
     */
    private T params;
}
