package com.example.silkmall.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.example.silkmall.repository")
@EnableTransactionManagement
public class ApplicationConfig {
    // 配置应用程序级别的Bean和设置
}