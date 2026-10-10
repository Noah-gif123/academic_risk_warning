package com.example.redchina.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源配置（让前端能访问素材图片、上传文件）
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射素材图片路径（数据库中存储的/image/**路径对应本地文件夹）
        registry.addResourceHandler("/material/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/src/main/resources/static/material/");
        // 映射上传的设计文件路径
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + System.getProperty("user.dir") + "/src/main/resources/static/upload/");
    }
}
