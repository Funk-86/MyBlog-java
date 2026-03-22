package org.example.myblog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * 全局 CORS 配置（含静态资源 /post_video、/user_img、/post_img）
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的前端地址（开发 + 线上）
        config.addAllowedOrigin("http://localhost:5173");  // Vite / H5 开发
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedOrigin("http://localhost:3001");  // MyBlog Admin 管理端
        config.addAllowedOrigin("http://localhost:3002");
        config.addAllowedOrigin("http://127.0.0.1:5173");
        config.addAllowedOrigin("https://myblog-vue.zeabur.app");
        // 兼容 Zeabur 所有子域名（H5、管理端、预览等）
        config.addAllowedOriginPattern("https://*.zeabur.app");
        config.addAllowedOriginPattern("http://*.zeabur.app");
        config.addAllowedOriginPattern("http://localhost:*");
        config.addAllowedOriginPattern("http://127.0.0.1:*");
        config.setAllowCredentials(true);
        config.addAllowedHeader(CorsConfiguration.ALL);
        config.addAllowedMethod(CorsConfiguration.ALL);
        // 视频 Range 请求需要暴露这些头，避免 ERR_CACHE_OPERATION_NOT_SUPPORTED / 播放失败
        config.setExposedHeaders(Arrays.asList("Content-Length", "Content-Range", "Accept-Ranges"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 全局应用，包含 /post_video、/user_img、/post_img 等静态资源
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

