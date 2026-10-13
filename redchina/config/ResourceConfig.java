package com.example.redchina.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ResourceConfig implements WebMvcConfigurer {
    // 外部图片存储目录（Windows 示例，Linux 改为 /home/culture-images/upload/）
    private static final String UPLOAD_DIR = "file:D:/culture-images/upload/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射 /upload/** 路径到外部目录
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(UPLOAD_DIR)
                .setCachePeriod(3600); // 缓存1小时，优化性能
    }
}