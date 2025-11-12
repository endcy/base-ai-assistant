package com.assistant.service.common.executor;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 线程构造和执行工具
 * 标准创建线程(池)和异步执行方法的工具类
 *
 * @author endcy
 * @since 2025/08/04 21:05:13
 */
@Slf4j
public class ServiceThreadExecutor {

    /**
     * 最大工作队列，不使用默认最大值，设置任务数量不超过100W
     */
    private static final int MAX_WORK_QUEUE_SIZE = 1000000;

    private ServiceThreadExecutor() {
    }

    /**
     * 无参无返回
     */
    public static void asyncExecute(TaskRunnable function) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> injectLogContext(context, function)).exceptionally(e -> {
            // 捕获并记录异常
            log.error("function execute failed", e);
            return null;
        });
    }

    /**
     * 无参无返回 最少延迟0.5秒执行
     */
    public static <T> void asyncDelayExecute(TaskRunnable function, long millSecond) {
        //复制主线程MDC关联上下文
        Map<String, String> context = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> {
            ThreadUtil.sleep(Math.max(500, millSecond));
            injectLogContext(context, function);
        });
    }

    /**
     * 异步执行消费方法，参数为T类型无返回
     *
     * @param function 方法名称，如 ServiceThreadExecutor::test 表示ServiceThreadExecutor类中的test方法，该方法参数类型匹配为T
     * @param arg      function方法的参数类型
     */
    public static <T> void asyncExecute(Consumer<T> function, T arg) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> injectLogContext(context, function, arg));
    }

    /**
     * 异步执行消费方法，参数为T类型无返回，最少延迟0.5秒执行
     */
    public static <T> void asyncDelayExecute(Consumer<T> function, T arg, long millSecond) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> {
            ThreadUtil.sleep(Math.max(500, millSecond));
            injectLogContext(context, function, arg);
        });
    }

    /**
     * 无参有返回
     */
    public static <R> void asyncExecute(Supplier<R> function) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> injectLogContext(context, function));
    }

    /**
     * 无参有返回 最少延迟0.5秒执行
     *
     * @param function 方法名称，如 ServiceThreadExecutor::test 表示ServiceThreadExecutor类中的test方法，该方法返回类型为T
     */
    public static <R> void asyncDelayExecute(Supplier<R> function, long millSecond) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        CompletableFuture.runAsync(() -> {
            ThreadUtil.sleep(Math.max(500, millSecond));
            injectLogContext(context, function);
        });
    }

    /**
     * 创建单个守护线程池 执行消费方法，接口方法无参无返回值
     */
    public static ThreadPoolTaskExecutor startSingleThreadInExecutor(@NotNull TaskRunnable function) {
        String consumerThreadPrefix = "single-pool-" + RandomUtil.randomInt(1000, 9999);
        ThreadPoolTaskExecutor executor = createThreadPoolExecutor(1, 1, null, MAX_WORK_QUEUE_SIZE,
                consumerThreadPrefix, new ThreadPoolExecutor.AbortPolicy());
        executor.setDaemon(true);
        executor.initialize();
        executor.execute(function::run);
        return executor;
    }

    /**
     * 创建指定线程数异步线程池
     */
    public static ThreadPoolTaskExecutor createSimpleThreadExecutor(int corePoolSize, int maxPoolSize, String consumerThreadPrefix) {
        return createThreadPoolExecutor(corePoolSize, maxPoolSize, null, MAX_WORK_QUEUE_SIZE,
                consumerThreadPrefix, new ThreadPoolExecutor.AbortPolicy());
    }


    /**
     * 创建指定线程数异步定时调度线程池
     */
    public static ThreadPoolTaskScheduler createSimpleScheduleExecutor(int maxPoolSize, String consumerThreadPrefix) {
        return createScheduleThreadPoolExecutor(maxPoolSize, consumerThreadPrefix, new ThreadPoolExecutor.AbortPolicy());
    }

    public static ThreadPoolTaskExecutor createThreadPoolExecutor(int corePoolSize,
                                                                  int maximumPoolSize,
                                                                  @Nullable Integer keepAliveSeconds,
                                                                  @Nullable Integer queueCapacity,
                                                                  @Nullable String consumerThreadName,
                                                                  @Nullable RejectedExecutionHandler rejectedExecutionHandler
    ) {
        ThreadPoolTaskExecutor poolTaskExecutor = new MdcMultipleThreadPoolTaskExecutor();
        poolTaskExecutor.setCorePoolSize(corePoolSize);
        poolTaskExecutor.setMaxPoolSize(maximumPoolSize);
        poolTaskExecutor.setQueueCapacity(ObjectUtil.defaultIfNull(queueCapacity, 10000));
        poolTaskExecutor.setKeepAliveSeconds(ObjectUtil.defaultIfNull(keepAliveSeconds, 10));
        if (StrUtil.isNotEmpty(consumerThreadName)) {
            poolTaskExecutor.setThreadNamePrefix(consumerThreadName);
        }
        poolTaskExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);
        poolTaskExecutor.initialize();
        return poolTaskExecutor;
    }

    public static ThreadPoolTaskScheduler createScheduleThreadPoolExecutor(int maximumPoolSize,
                                                                           @Nullable String consumerThreadName,
                                                                           @Nullable RejectedExecutionHandler rejectedExecutionHandler
    ) {
        ThreadPoolTaskScheduler poolTaskExecutor = new MdcScheduleThreadPoolTaskExecutor();
        poolTaskExecutor.setPoolSize(maximumPoolSize);
        if (StrUtil.isNotEmpty(consumerThreadName)) {
            poolTaskExecutor.setThreadNamePrefix(consumerThreadName);
        }
        poolTaskExecutor.setRejectedExecutionHandler(rejectedExecutionHandler);
        poolTaskExecutor.setDaemon(true);
        poolTaskExecutor.initialize();
        return poolTaskExecutor;
    }

    private static <T> void injectLogContext(Map<String, String> context, Consumer<T> function, T arg) {
        if (null != context) {
            //主线程MDC赋予子线程
            MDC.setContextMap(context);
        }
        try {
            function.accept(arg);
        } finally {
            try {
                MDC.clear();
            } catch (Exception e) {
                log.warn("MDC clear exception：{}", e.getMessage());
            }
        }
    }

    private static void injectLogContext(Map<String, String> context, TaskRunnable function) {
        if (null != context) {
            //主线程MDC赋予子线程
            MDC.setContextMap(context);
        }
        try {
            function.run();
        } finally {
            try {
                MDC.clear();
            } catch (Exception e) {
                log.warn("MDC clear exception：{}", e.getMessage());
            }
        }
    }

    private static <R> void injectLogContext(Map<String, String> context, Supplier<R> function) {
        if (null != context) {
            //主线程MDC赋予子线程
            MDC.setContextMap(context);
        }
        try {
            function.get();
        } finally {
            try {
                MDC.clear();
            } catch (Exception e) {
                log.warn("MDC clear exception：{}", e.getMessage());
            }
        }
    }

    /**
     * 自适应并行处理
     * 鉴于parallelStream + ForkJoinPool处理的并行线程池太过拉胯，无法自动取消任务，也无法控制富余执行线程，所以使用自定义封装并行线程池的处理方案
     * 当线程池不够用时，只取前10条数据，避免阻塞
     * 任务执行超时时间超过5秒取消执行
     * 任务单元隔离异常，规避影响其他业务
     *
     * @param executor      不同业务处理的线程池
     * @param toProcessList 待处理数据列表
     * @param consumer      自定义消费方法
     * @param <T>           .
     * @implNote 仅建议在非关键任务中使用，如web调用查询接口的数据处理等
     */
    public static <T> void parallelProcess(ThreadPoolTaskExecutor executor,
                                           Collection<T> toProcessList,
                                           Consumer<T> consumer) {
        // 任务执行超时时间超过5秒取消执行
        parallelProcess(executor, toProcessList, consumer, 5, true);
    }

    public static <T> void parallelProcessAll(ThreadPoolTaskExecutor executor,
                                              Collection<T> toProcessList,
                                              Consumer<T> consumer) {
        // 任务执行超时时间超过5秒取消执行
        parallelProcess(executor, toProcessList, consumer, 5, false);
    }

    public static <T> void parallelProcessAll(
            int maxProcessSeconds,
            ThreadPoolTaskExecutor executor,
            Collection<T> toProcessList,
            Consumer<T> consumer) {
        parallelProcess(executor, toProcessList, consumer, maxProcessSeconds, false);
    }

    /**
     * @param maxProcessSeconds 最大超时时间
     * @param abandonOver       超线程量任务是否丢弃
     * @param <T>               .
     */
    public static <T> void parallelProcess(
            ThreadPoolTaskExecutor executor,
            Collection<T> toProcessList,
            Consumer<T> consumer,
            int maxProcessSeconds,
            boolean abandonOver) {

        // 线程不足时降级：仅处理前10条
        if (abandonOver && executor.getThreadPoolExecutor().getQueue().size() > executor.getMaxPoolSize()) {
            log.warn("ThreadPoolTaskExecutor busy, fallback to first 10 items");
            toProcessList = toProcessList.stream().limit(10).collect(Collectors.toList());
        }

        List<Future<?>> futures = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(toProcessList.size());

        try {
            // 提交所有任务
            for (T item : toProcessList) {
                futures.add(executor.submit(() -> {
                    try {
                        consumer.accept(item);
                    } catch (Exception e) {
                        log.error("Task process error: {}", item, e);
                    } finally {
                        latch.countDown();
                    }
                }));
            }

            // 统一超时控制
            if (!latch.await(maxProcessSeconds, TimeUnit.SECONDS)) {
                log.error("Overall processing timeout, cancelling remaining tasks");
                futures.forEach(f -> f.cancel(true));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            futures.forEach(f -> f.cancel(true));
            log.error("Processing interrupted: {}", e.getMessage());
        }
    }
}
