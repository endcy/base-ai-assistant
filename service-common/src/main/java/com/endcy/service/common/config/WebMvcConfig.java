package com.endcy.service.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * WebMvc 配置 —— 异步线程池。
 *
 * @author endcy
 * @since 2026/08/08
 */
@EnableAsync
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig {

    @Bean("mvcThreadPoolTaskExecutor")
    public ThreadPoolTaskExecutor mvcThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("mvc-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer(ThreadPoolTaskExecutor mvcThreadPoolTaskExecutor) {
        return new WebMvcConfigurer() {
            @Override
            public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
                configurer.setTaskExecutor(mvcThreadPoolTaskExecutor);
                configurer.setDefaultTimeout(60000);
            }
        };
    }
}
