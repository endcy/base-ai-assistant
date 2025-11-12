package com.assistant.ai.config;

import com.assistant.service.common.constant.MQConstant;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * ...
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Configuration
public class EnergyAiRabbitMqConfig {

    /**
     * 交换机名称，点对点的消费形式一次声明，减少重复声明
     */
    @Bean("adminDirectExchange")
    @SuppressWarnings("all")
    public AbstractExchange adminDirectExchange() {
        if (!MQConstant.MSG_TO_AI_API.contains(MQConstant.DELAYED_EXCHANGE_KEYWORD)) {
            return new DirectExchange(MQConstant.MSG_TO_AI_API, true, false);
        }
        Map<String, Object> map = new HashMap<>();
        map.put("x-delayed-type", "direct");
        return new CustomExchange(MQConstant.MSG_TO_AI_API, "x-delayed-message", true, false, map);
    }

    @Bean
    public Queue apiDocumentRefreshQueue() {
        return new Queue(MQConstant.MQ_API_REFRESH_DUCOMENT, true);
    }

    @Bean
    public Binding adminReportProcessBinding() {
        return BindingBuilder.bind(apiDocumentRefreshQueue()).to(adminDirectExchange()).with(MQConstant.MQ_API_REFRESH_DUCOMENT).noargs();
    }

}
