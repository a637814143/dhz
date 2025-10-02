package com.example.silkmall.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CoreConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许所有请求路径进行跨域访问   
        registry.addMapping("/**")
                // 允许来自指定源的请求
                .allowedOriginPatterns("http://localhost:5173")
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