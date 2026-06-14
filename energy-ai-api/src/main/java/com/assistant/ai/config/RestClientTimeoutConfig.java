package com.assistant.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP客户端超时配置
 * 解决DashScope多模态请求默认10秒超时问题
 *
 * @author endcy
 * @date 2026/06/14
 */
@Configuration
@Slf4j
public class RestClientTimeoutConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(60));
        requestFactory.setReadTimeout(Duration.ofSeconds(180));

        log.info("########## Custom RestClient.Builder with HTTP timeout: connect=60s, read=180s");

        return RestClient.builder()
                         .requestFactory(requestFactory);
    }

}
