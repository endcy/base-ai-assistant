package com.assistant.ai.listener;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.assistant.ai.listener.strategy.IAiMsgConsumerStrategy;
import com.assistant.service.domain.bo.BaseMqMsgRequest;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 通知消息消费者
 * 执行策略应该以业务作为划分维度，因为不同的业务可能有多种且多个推送渠道
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Slf4j
@Component
@RabbitListener(queues = "energy_ai_topic")
@RequiredArgsConstructor
@SuppressWarnings({"rawtypes", "unchecked"})
public class AiMessageConsumer {
    private final Map<String, IAiMsgConsumerStrategy> aiMsgStrategyMap;

    @RabbitHandler
    public void consume(String msg) {
        if (StrUtil.isEmpty(msg)) {
            return;
        }
        try {
            log.info("consume message:{}", msg);
            BaseMqMsgRequest baseMsgParam = JSONUtil.toBean(msg, BaseMqMsgRequest.class);
            if (baseMsgParam == null || baseMsgParam.getBizType() == null) {
                return;
            }
            service(baseMsgParam, msg);
        } catch (Exception e) {
            log.error("consume notify error, msg:{}", msg, e);
        }
    }

    /**
     * 消费具体业务
     */
    @SneakyThrows
    private void service(@NonNull BaseMqMsgRequest aiMsgParam, String msg) {
        String strategyBeanName = IAiMsgConsumerStrategy.BEAN_NAME_PREFIX + aiMsgParam.getBizType().getCode();
        IAiMsgConsumerStrategy<BaseMqMsgRequest> aiMsgStrategy = aiMsgStrategyMap.get(strategyBeanName);

        if (aiMsgStrategy == null) {
            log.error("consumer service error, msgParam is null");
            return;
        }

        //参数值还原
        BaseMqMsgRequest msgParam = JSONUtil.toBean(msg, aiMsgParam.getBizType().getParamClass());
        boolean sendSuccess = aiMsgStrategy.handlerSuccess(msgParam);
        log.info("consumer service success status:{}", sendSuccess);
        boolean afterStatus = aiMsgStrategy.afterProcess(msgParam);
        log.info("consumer service after status:{}", afterStatus);
    }

}
