// FileUtil.java - 文件工具类
package com.ihdrs.backend.common.utils;

import com.ihdrs.backend.common.constants.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
@Component
public class FileUtil {

    /**
     * 保存上传的图片文件
     */
    public String saveImageFile(MultipartFile file) throws IOException {
        // 验证文件类型
        if (!isValidImageType(file.getContentType())) {
            throw new IllegalArgumentException("不支持的文件类型: " + file.getContentType());
        }

        // 验证文件大小
        if (file.getSize() > Constants.MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制: " + file.getSize());
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + extension;

        // 确保上传目录存在
        Path uploadPath = Paths.get(Constants.UPLOAD_PATH);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // 保存文件
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath);

        log.info("文件保存成功: {}", filePath.toString());
        return filePath.toString();
    }

    /**
     * 验证图片文件类型
     */
    private boolean isValidImageType(String contentType) {
        return Arrays.asList(Constants.ALLOWED_IMAGE_TYPES).contains(contentType);
    }

    /**
     * 计算文件MD5哈希值
     */
    public String calculateFileHash(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("计算文件哈希失败", e);
            return null;
        }
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("删除文件失败: {}", filePath, e);
            return false;
        }
    }
}