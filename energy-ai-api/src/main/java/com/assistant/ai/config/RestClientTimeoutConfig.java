package com.assistant.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端配置
 * 使用Apache HttpClient5连接池，解决SimpleClientHttpRequestFactory无连接池导致的慢请求问题
 *
 * @author endcy
 * @date 2026/06/17
 */
@Configuration
@Slf4j
public class RestClientTimeoutConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.setDefaultConnectionConfig(ConnectionConfig.custom()
                                                                     .setConnectTimeout(Timeout.of(10, TimeUnit.SECONDS))
                                                                     .setSocketTimeout(Timeout.of(180, TimeUnit.SECONDS))
                                                                     .build());

        RequestConfig requestConfig = RequestConfig.custom()
                                                   .setConnectionRequestTimeout(Timeout.of(10, TimeUnit.SECONDS))
                                                   .setResponseTimeout(Timeout.of(180, TimeUnit.SECONDS))
                                                   .build();

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                                                          .setConnectionManager(connectionManager)
                                                          .setDefaultRequestConfig(requestConfig)
                                                          .evictIdleConnections(TimeValue.of(60, TimeUnit.SECONDS))
                                                          .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        log.info("########## Custom RestClient.Builder with HttpComponents connection pool: " +
                "maxTotal=50, maxPerRoute=20, connectTimeout=10s, readTimeout=180s");

        return RestClient.builder()
                         .requestFactory(requestFactory);
    }

}
