package com.endcy.ai.rpc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * RPC 组件配置
 *
 * @author endcy
 * @since 2026/08/13
 */
@Slf4j
@Configuration
@ComponentScan(basePackages = {"com.endcy.ai.rpc"})
@RequiredArgsConstructor
public class RpcComponentConfig {
}
