// ImageUtil.java - 图像处理工具类
package com.ihdrs.backend.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Component
public class ImageUtil {

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