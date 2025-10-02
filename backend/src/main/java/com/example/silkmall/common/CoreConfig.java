package com.example.silkmall.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CoreConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CoreConfig(@Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:4173,http://127.0.0.1:5173,http://127.0.0.1:4173}") String[] allowedOrigins) {
        String[] sanitizedOrigins = Arrays.stream(allowedOrigins)
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        this.allowedOrigins = sanitizedOrigins.length > 0 ? sanitizedOrigins
                : new String[]{"http://localhost:5173", "http://localhost:4173", "http://127.0.0.1:5173", "http://127.0.0.1:4173"};
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许所有请求路径进行跨域访问
        registry.addMapping("/**")
                // 允许来自指定源的请求
                .allowedOriginPatterns(allowedOrigins)
                // 允许所有请求方法（GET, POST, PUT, DELETE等）
                .allowedMethods("*")
                // 允许所有请求头
                .allowedHeaders("*")
                // 允许发送Cookie信息
                .allowCredentials(true)
                // 设置预检请求的有效期（秒）
                .maxAge(3600);
    }
}