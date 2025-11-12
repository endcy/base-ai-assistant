package com.assistant.service.common.config.mq;

import cn.hutool.core.util.StrUtil;
import com.rabbitmq.client.impl.DefaultCredentialsProvider;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * ...
 *
 * @author endcy
 * @since 2025/08/04 21:00:00
 */
@Configuration
public class RabbitMqConfig {

    /**
     * 实例Id 从阿里云AMQP控制台获取
     */
    @Value("${spring.rabbitmq.aliyun.instance:#{null}}")
    private String instance;

    @Resource
    private RabbitProperties rabbitProperties;

    @Primary
    @Bean
    public ConnectionFactory getConnectionFactory() {
        com.rabbitmq.client.ConnectionFactory rabbitConnectionFactory =
                new com.rabbitmq.client.ConnectionFactory();
        rabbitConnectionFactory.setHost(rabbitProperties.getHost());
        rabbitConnectionFactory.setPort(rabbitProperties.getPort());
        rabbitConnectionFactory.setVirtualHost(rabbitProperties.getVirtualHost());
        if (StrUtil.isNotEmpty(instance)) {
            AliyunCredentialsProvider credentialsProvider = new AliyunCredentialsProvider(
                    rabbitProperties.getUsername(), rabbitProperties.getPassword(), instance);
            rabbitConnectionFactory.setCredentialsProvider(credentialsProvider);
        } else {
            DefaultCredentialsProvider credentialsProvider = new DefaultCredentialsProvider(
                    rabbitProperties.getUsername(), rabbitProperties.getPassword());
            rabbitConnectionFactory.setCredentialsProvider(credentialsProvider);
        }
        rabbitConnectionFactory.setAutomaticRecoveryEnabled(true);
        rabbitConnectionFactory.setNetworkRecoveryInterval(5000);
        return new CachingConnectionFactory(rabbitConnectionFactory);
    }

}
