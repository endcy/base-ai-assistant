package com.assistant.ai.rpc.config;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * RPC 组件配置（Dubbo / Feign / Nacos 注册发现）
 * <p>
 * 仅在 ai.rpc.enabled=true 时激活微服务相关注解。
 * 开源单机部署场景下可设为 false 或直接不配置，跳过 Dubbo/Feign/Nacos 初始化。
 * </p>
 *
 * @author endcy
 * @since 2026/06/11
 */
@Configuration
@ConditionalOnProperty(name = "ai.rpc.enabled", havingValue = "true", matchIfMissing = false)
@EnableDubbo(scanBasePackages = {"com.assistant.ai.rpc"})
@EnableDiscoveryClient
@EnableFeignClients
public class RpcComponentConfig {
}
