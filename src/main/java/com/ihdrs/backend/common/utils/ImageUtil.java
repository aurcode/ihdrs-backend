// ImageUtil.java - 图像处理工具类
package com.ihdrs.backend.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Component
public class ImageUtil {

    @Value("${file.upload.path}")
    private String uploadBasePath;

    /**
     * 保存识别图片，并返回可访问的相对路径
     * 在你当前 WebMvcConfig 下，对外访问 URL 为：
     *   http(s)://<host>:<port>/uploads/recognitions/xxxx.png
     * 对应 DB 中 image_path 存：/uploads/recognitions/xxxx.png
     *
     * @param imageData 图片二进制数据
     * @param imageHash 图片哈希（用于生成稳定文件名）
     * @return URL 相对路径，例如：/uploads/recognitions/digit-<hash>.png；失败时返回 null
     */
    public String saveRecognitionImage(byte[] imageData, String imageHash) {
        if (imageData == null || imageData.length == 0) {
            log.warn("saveRecognitionImage: imageData is empty");
            return null;
        }

        try {

            // 物理目录：uploadBasePath + /recognitions
            // 确保 uploadBasePath 末尾带 /
            String baseDir = uploadBasePath.endsWith(File.separator)
                    ? uploadBasePath
                    : uploadBasePath + File.separator;

            String dirPath = baseDir;
            Files.createDirectories(Paths.get(dirPath));

            // 使用 hash 生成文件名
            String safeHash = (imageHash != null && !imageHash.isBlank())
                    ? imageHash
                    : String.valueOf(System.currentTimeMillis());

            String fileName = "digit-" + safeHash + ".png";

            String filePath = dirPath + File.separator + fileName;

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(imageData);
            }

            String urlPath = "/api/uploads/" + fileName;

            log.info("Recognition image saved: physicalPath={}, urlPath={}", filePath, urlPath);

            return urlPath;
        } catch (IOException e) {
            log.error("Failed to save recognition image", e);
            return null;
        }
    }

    /**
     * 调整图像大小到28x28像素（MNIST标准）
     */
    public byte[] resizeToMNIST(byte[] imageData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
        BufferedImage originalImage = ImageIO.read(bais);

        // 转换为灰度图像
        BufferedImage grayImage = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = grayImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, 28, 28, null);
        g2d.dispose();

        // 转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(grayImage, "png", baos);
        return baos.toByteArray();
    }

    /**
     * 图像预处理：二值化
     */
    public BufferedImage binarizeImage(BufferedImage image) {
        BufferedImage binaryImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = binaryImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return binaryImage;
    }

    /**
     * 提取图像中心区域
     */
    public BufferedImage extractCenterRegion(BufferedImage image, int targetSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int minDimension = Math.min(width, height);

        int x = (width - minDimension) / 2;
        int y = (height - minDimension) / 2;

        BufferedImage croppedImage = image.getSubimage(x, y, minDimension, minDimension);

        // 调整到目标大小
        BufferedImage resizedImage = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(croppedImage, 0, 0, targetSize, targetSize, null);
        g2d.dispose();

        return resizedImage;
    }
}