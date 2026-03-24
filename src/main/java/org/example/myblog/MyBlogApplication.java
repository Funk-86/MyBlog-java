package org.example.myblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class MyBlogApplication {

    public static void main(String[] args) {
        // 统一后端时间基准为北京时间，避免 LocalDateTime.now() 在 UTC 容器中偏移 8 小时
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(MyBlogApplication.class, args);
    }

}
