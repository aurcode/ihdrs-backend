// WebConfig.java - Web配置
package com.ihdrs.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置文件上传路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");

        // 配置模型文件路径
        registry.addResourceHandler("/models/**")
                .addResourceLocations("file:models/");
    }
}