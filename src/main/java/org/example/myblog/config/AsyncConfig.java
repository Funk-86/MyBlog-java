package org.example.myblog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启用 Spring 异步支持，用于 AliyunGreenService 异步审核帖子内容。
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}

