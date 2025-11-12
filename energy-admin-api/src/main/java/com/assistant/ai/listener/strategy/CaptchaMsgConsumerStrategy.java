package com.assistant.ai.listener.strategy;

import cn.hutool.core.util.StrUtil;
import com.assistant.service.domain.bo.ExMqMsgRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 具体执行策略
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Slf4j
@Service(IAiMsgConsumerStrategy.BEAN_NAME_PREFIX + 1)
@RequiredArgsConstructor
public class CaptchaMsgConsumerStrategy implements IAiMsgConsumerStrategy<ExMqMsgRequest> {


    @Override
    public boolean verifyParam(ExMqMsgRequest request) {
        if (request == null) {
            log.error("verifyParam error, request is null");
            return false;
        }

        if (StrUtil.isBlank(request.getParams())) {
            log.error("verifyParam error, Code is null");
            return false;
        }
        return true;
    }

    @Override
    public boolean handlerSuccess(ExMqMsgRequest request) {
        return true;
    }

    @Override
    public boolean afterProcess(ExMqMsgRequest request) {
        return false;
    }


}
