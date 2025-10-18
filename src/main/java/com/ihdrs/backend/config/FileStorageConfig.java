// FileStorageConfig.java - 文件存储配置
package com.ihdrs.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.File;

@Configuration
@ConfigurationProperties(prefix = "file")
@Getter
@Setter
public class FileStorageConfig {

    private Upload upload = new Upload();
    private Model model = new Model();

    @PostConstruct
    public void init() {
        // 确保目录存在
        createDirectoryIfNotExists(upload.getPath());
        createDirectoryIfNotExists(model.getPath());
    }

    private void createDirectoryIfNotExists(String path) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @Getter
    @Setter
    public static class Upload {
        private String path = "./uploads/";
        private Long maxSize = 5242880L; // 5MB
        private String allowedTypes = "image/png,image/jpeg,image/jpg";
    }

    @Getter
    @Setter
    public static class Model {
        private String path = "./models/";
    }
}