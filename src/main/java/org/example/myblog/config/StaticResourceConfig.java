package org.example.myblog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源映射：把本地 user_img 目录暴露为 /user_img/**
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path userDir = Paths.get("user_img").toAbsolutePath().normalize();
        registry.addResourceHandler("/user_img/**")
                .addResourceLocations(userDir.toUri().toString())
                // 上传后可能会出现“刚好在文件生成间隙请求”的情况
                // 前端会在失败时自动带时间戳重试，因此可以适当缓存提升加载速度
                .setCachePeriod(3600);
        Path postDir = Paths.get("post_img").toAbsolutePath().normalize();
        registry.addResourceHandler("/post_img/**")
                .addResourceLocations(postDir.toUri().toString())
                .setCachePeriod(3600);
        Path videoDir = Paths.get("post_video").toAbsolutePath().normalize();
        registry.addResourceHandler("/post_video/**")
                .addResourceLocations(videoDir.toUri().toString())
                .setCachePeriod(3600);
    }
}

