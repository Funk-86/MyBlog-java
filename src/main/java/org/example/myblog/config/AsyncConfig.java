package org.example.myblog.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 启用 Spring 异步支持，用于 AliyunGreenService 异步审核帖子内容。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${myblog.async.core-pool-size:2}")
    private int corePoolSize;

    @Value("${myblog.async.max-pool-size:4}")
    private int maxPoolSize;

    @Value("${myblog.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${myblog.async.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    /**
     * 有界异步线程池：防止高峰期无限扩线程造成内存压力。
     * 被 @Async 使用（默认 Bean 名 taskExecutor）。
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("myblog-async-");
        executor.setCorePoolSize(Math.max(1, corePoolSize));
        executor.setMaxPoolSize(Math.max(corePoolSize, maxPoolSize));
        executor.setQueueCapacity(Math.max(10, queueCapacity));
        executor.setKeepAliveSeconds(Math.max(30, keepAliveSeconds));
        // 队列满时由提交线程执行，避免任务无限堆积导致 OOM
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

