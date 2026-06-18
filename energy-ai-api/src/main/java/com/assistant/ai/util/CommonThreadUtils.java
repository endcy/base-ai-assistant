package com.assistant.ai.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ...
 *
 * @author endcy
 * @date 2026/6/17 16:40:35
 */
public class CommonThreadUtils {


    /**
     * AI调用专用线程池
     * 1. 线程池不允许使用Executors创建，通过ThreadPoolExecutor方式创建
     * 2. 线程名必须有业务含义，便于问题排查
     * 3. 核心参数：corePoolSize=4, maxPoolSize=20, 空闲存活60s, 有界队列容量1000
     */
    public static final ThreadPoolExecutor AI_TASK_EXECUTOR = new ThreadPoolExecutor(
            4,
            20,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "ai-task-" + threadNumber.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

}
