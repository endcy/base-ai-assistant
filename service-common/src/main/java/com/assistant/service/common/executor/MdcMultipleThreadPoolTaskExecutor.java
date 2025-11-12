package com.assistant.service.common.executor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * 继承线程池执行器 ThreadPoolTaskExecutor
 * 支持给log上下文MDC赋值
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
@Slf4j
public class MdcMultipleThreadPoolTaskExecutor extends ThreadPoolTaskExecutor {

    /**
     * 接口请求开启的异步线程会调用下述方法
     */
    @Override
    public void execute(@NonNull Runnable task) {
        //复制主线程MDC关联上下文
        Map<String, String> context = MDC.getCopyOfContextMap();
        super.execute(() -> {
            if (null != context) {
                //主线程MDC赋予子线程
                MDC.setContextMap(context);
            }
            try {
                task.run();
            } finally {
                try {
                    MDC.clear();
                } catch (Exception e) {
                    log.warn("MDC clear exception：{}", e.getMessage());
                }
            }
        });
    }

    /**
     * 定时任务会调用下述方法
     */
    @Override
    public <T> Future<T> submit(@NonNull Callable<T> task) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return super.submit(() -> {
            if (null != context) {
                //主线程MDC赋予子线程
                MDC.setContextMap(context);
            }
            try {
                return task.call();
            } finally {
                try {
                    MDC.clear();
                } catch (Exception e) {
                    log.warn("MDC clear exception：{}", e.getMessage());
                }
            }
        });
    }

}
