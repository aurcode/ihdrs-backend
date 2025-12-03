package com.ihdrs.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.dataset.path:./downloads/}")
    private String datasetPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 上传文件映射
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);

        // 训练集文件映射
        registry.addResourceHandler("/downloads/**")
                .addResourceLocations("file:" + datasetPath);

        System.out.println("Static files mapped: uploads=" + uploadPath + ", datasets=" + datasetPath);
    }
}