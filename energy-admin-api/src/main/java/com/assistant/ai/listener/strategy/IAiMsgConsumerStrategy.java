package com.assistant.ai.listener.strategy;

import com.assistant.service.domain.bo.BaseMqMsgRequest;

/**
 * 业务执行策略基类
 * bean名称以aiMsgStrategy_开头，后面跟着业务枚举类型code
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
public interface IAiMsgConsumerStrategy<T extends BaseMqMsgRequest> {

    String BEAN_NAME_PREFIX = "aiMsgStrategy_";

    boolean verifyParam(T request);

    boolean handlerSuccess(T request);

    boolean afterProcess(T request);
}
