package com.endcy.ai.rpc.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient配置
 *
 * @author endcy
 * @since 2025/12/20
 */
@Slf4j
@ConditionalOnProperty(name = "ai.service.client.enabled", havingValue = "true")
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {

    @Value("${ai.service.energy-ai-api:http://localhost:9051}")
    private String aiApiBaseUrl;

    @Bean("aiWebClient")
    public WebClient webClient() {
        return WebClient.builder().baseUrl(aiApiBaseUrl).build();
    }

}
