package com.assistant.ai.rpc.domain.base;

import lombok.Data;

import java.io.Serializable;

/**
 * 基类 请求消息规约
 * 涉及网络传输，字段和实体越简约越高效
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Data
public abstract class BaseReqMsgDTO implements Serializable {
    private static final long serialVersionUID = 7555168977714964177L;

    /**
     * 时间戳
     */
    private Long ts;

    /**
     * 构造实例，初始化时间戳本身不随业务变更
     */
    protected BaseReqMsgDTO() {
        this.ts = System.currentTimeMillis();
    }

}
