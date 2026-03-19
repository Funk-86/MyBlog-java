package org.example.myblog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 全局 CORS 配置
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        // 允许的前端地址（开发 + 线上）
        config.addAllowedOrigin("http://localhost:5173");  // Vite 默认端口
        config.addAllowedOrigin("http://localhost:3001");  // MyBlog Admin 管理端
        config.addAllowedOrigin("http://localhost:3002");  // MyBlog Admin 管理端（备用端口）
        config.addAllowedOrigin("https://myblog-vue.zeabur.app"); // Zeabur 前端域名
        // 兼容后续新建的 zeabur 子域名（例如预览环境）
        config.addAllowedOriginPattern("https://*.zeabur.app");
        config.setAllowCredentials(true);
        config.addAllowedHeader(CorsConfiguration.ALL);
        config.addAllowedMethod(CorsConfiguration.ALL);
        // 预检请求的缓存时间（秒）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

