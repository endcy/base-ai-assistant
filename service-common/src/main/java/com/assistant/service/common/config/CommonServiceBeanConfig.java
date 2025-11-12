package com.assistant.service.common.config;

import com.assistant.service.common.executor.MdcMultipleThreadPoolTaskExecutor;
import com.assistant.service.common.executor.ThreadScopeCleanerDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ...
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
@Configuration
@ComponentScan(basePackages = {"com.assistant.service.common"})
public class CommonServiceBeanConfig {

    /**
     * 用于执行异步业务的公共线程池
     * 使用方法： @Async("commonTaskExecutor")
     */
    @Bean("commonTaskExecutor")
    public Executor commonTaskExecutor() {
        ThreadPoolTaskExecutor executor = new MdcMultipleThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(10000);
        executor.setKeepAliveSeconds(10);
        executor.setThreadNamePrefix("execService-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setTaskDecorator(new ThreadScopeCleanerDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * 用于执行异步持久化处理的公共线程池
     * 使用方法： @Async("dbTaskExecutor")
     */
    @Bean("dbTaskExecutor")
    public Executor dbTaskExecutor() {
        ThreadPoolTaskExecutor executor = new MdcMultipleThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(200);
        executor.setQueueCapacity(10000);
        executor.setKeepAliveSeconds(10);
        executor.setThreadNamePrefix("asyncDbService-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setTaskDecorator(new ThreadScopeCleanerDecorator());
        executor.initialize();
        return executor;
    }

}
