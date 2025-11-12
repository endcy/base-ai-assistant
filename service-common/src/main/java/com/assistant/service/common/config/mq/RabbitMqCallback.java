package com.assistant.service.common.config.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 正常确认/非正常消息回调
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
@Slf4j
@Component
public class RabbitMqCallback implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String id = correlationData != null ? correlationData.getId() : "";
        if (!ack) {
            log.warn("Exchange not receive msg id:{}, reason:{}", id, cause);
        }
    }

    @Override
    public void returnedMessage(ReturnedMessage returned) {
        log.error("Exchange callback msg id:{}, route key:{}, reason:{}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
    }
}
