package com.ihdrs.backend.service;

import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.common.utils.ImageUtil;
import com.ihdrs.backend.dto.request.TrainingDatasetRequest;
import com.ihdrs.backend.dto.response.TrainingDatasetResponse;
import com.ihdrs.backend.entity.FeedbackData;
import com.ihdrs.backend.entity.RecognitionRecord;
import com.ihdrs.backend.repository.FeedbackDataRepository;
import com.ihdrs.backend.repository.RecognitionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingDatasetService {

    private final FeedbackDataRepository feedbackRepository;
    private final RecognitionRecordRepository recordRepository;
    private final ImageUtil imageUtil;

    @Value("${file.upload.path}")
    private String uploadBasePath;

    @Value("${file.dataset.path:./downloads/}")
    private String datasetBasePath;

    /**
     * 预览可用于生成训练集的反馈统计
     */
    public Result<Map<String, Object>> previewTrainingData(TrainingDatasetRequest request) {
        try {
            List<FeedbackData> feedbackList = feedbackRepository.findAcceptedSingleDigitFeedback(
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getMinQualityScore() != null ? request.getMinQualityScore().intValue() : null
            );

            // 统计每个数字的图片数量
            Map<Integer, Integer> distribution = new HashMap<>();
            for (int i = 0; i <= 9; i++) {
                distribution.put(i, 0);
            }

            int validCount = 0;
            for (FeedbackData feedback : feedbackList) {
                // 检查关联的识别记录是否有有效图片
                RecognitionRecord record = recordRepository.findById(feedback.getRecordId()).orElse(null);
                if (record != null && record.getImagePath() != null) {
                    Integer digit = feedback.getCorrectResult();
                    distribution.put(digit, distribution.getOrDefault(digit, 0) + 1);
                    validCount++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalCount", validCount);
            result.put("distribution", distribution);
            result.put("feedbackList", feedbackList.size());

            return Result.success(result);
        } catch (Exception e) {
            log.error("预览训练数据失败", e);
            return Result.error(500, "预览训练数据失败: " + e.getMessage());
        }
    }

    /**
     * 生成训练集
     */
    public Result<TrainingDatasetResponse> generateTrainingDataset(TrainingDatasetRequest request, Long operatorId) {
        try {
            // 1.查询符合条件的反馈数据
            List<FeedbackData> feedbackList = feedbackRepository.findAcceptedSingleDigitFeedback(
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getMinQualityScore() != null ? request.getMinQualityScore().intValue() : null
            );

            if (feedbackList.isEmpty()) {
                return Result.error(400, "没有符合条件的反馈数据");
            }

            // 2.创建数据集目录结构
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String datasetName = request.getDatasetName() != null ?
                    request.getDatasetName() : "feedback_dataset_" + timestamp;

            String datasetDir = datasetBasePath + File.separator + datasetName;
            Path datasetPath = Paths.get(datasetDir);
            Files.createDirectories(datasetPath);

            // 创建0-9数字子目录
            for (int i = 0; i <= 9; i++) {
                Files.createDirectories(datasetPath.resolve(String.valueOf(i)));
            }

            // 3.复制图片到对应目录
            Map<Integer, Integer> classDistribution = new HashMap<>();
            int totalImages = 0;
            int successCount = 0;

            for (FeedbackData feedback : feedbackList) {
                try {
                    RecognitionRecord record = recordRepository.findById(feedback.getRecordId()).orElse(null);
                    if (record == null || record.getImagePath() == null) {
                        continue;
                    }

                    Integer correctDigit = feedback.getCorrectResult();
                    if (correctDigit == null || correctDigit < 0 || correctDigit > 9) {
                        continue;
                    }

                    // 获取原始图片路径
                    String imagePath = getPhysicalImagePath(record.getImagePath());
                    Path sourceFile = Paths.get(imagePath);

                    if (!Files.exists(sourceFile)) {
                        log.warn("图片文件不存在: {}", imagePath);
                        continue;
                    }

                    // 目标文件路径
                    String targetFileName = "feedback_" + feedback.getFeedbackId() + "_" +
                            record.getRecordId() + ".png";
                    Path targetFile = datasetPath.resolve(String.valueOf(correctDigit))
                            .resolve(targetFileName);

                    // 是否需要调整为MNIST格式
                    if (Boolean.TRUE.equals(request.getResizeToMNIST())) {
                        resizeAndSaveImage(sourceFile, targetFile);
                    } else {
                        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    classDistribution.put(correctDigit,
                            classDistribution.getOrDefault(correctDigit, 0) + 1);
                    successCount++;

                } catch (Exception e) {
                    log.error("处理反馈图片失败: feedbackId={}", feedback.getFeedbackId(), e);
                }
            }

            totalImages = successCount;

            if (totalImages == 0) {
                // 清理空目录
                deleteDirectory(datasetPath);
                return Result.error(400, "没有有效的图片可以生成训练集");
            }

            // 4.生成数据集描述文件
            generateDatasetInfo(datasetPath, datasetName, request.getDescription(),
                    classDistribution, totalImages, operatorId);

            // 5.如果需要ZIP格式，打包数据集
            String downloadUrl = null;
            if ("zip".equalsIgnoreCase(request.getExportFormat())) {
                String zipPath = datasetDir + ".zip";
                createZipFile(datasetPath, Paths.get(zipPath));
                downloadUrl = "/api/downloads/" + datasetName + ".zip";
            } else {
                downloadUrl = "/api/downloads/" + datasetName;
            }

            TrainingDatasetResponse response = TrainingDatasetResponse.builder()
                    .datasetName(datasetName)
                    .datasetPath(datasetDir)
                    .downloadUrl(downloadUrl)
                    .totalImages(totalImages)
                    .classDistribution(classDistribution)
                    .createTime(LocalDateTime.now())
                    .status("SUCCESS")
                    .message("训练集生成成功")
                    .build();

            log.info("训练集生成成功: name={}, totalImages={}, operatorId={}",
                    datasetName, totalImages, operatorId);

            return Result.success(response);

        } catch (Exception e) {
            log.error("生成训练集失败", e);
            return Result.error(500, "生成训练集失败: " + e.getMessage());
        }
    }

    /**
     * 将URL路径转换为物理文件路径
     */
    private String getPhysicalImagePath(String imagePath) {
        // imagePath 格式类似: /api/uploads/digit-xxx.png
        // 需要转换为物理路径
        if (imagePath.startsWith("/api/uploads/")) {
            String fileName = imagePath.substring("/api/uploads/".length());
            return uploadBasePath + File.separator + fileName;
        }
        return imagePath;
    }

    /**
     * 调整图片为28x28 MNIST格式并保存
     */
    private void resizeAndSaveImage(Path source, Path target) throws IOException {
        BufferedImage originalImage = ImageIO.read(source.toFile());

        // 转换为28x28灰度图像
        BufferedImage resizedImage = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, 28, 28, null);
        g2d.dispose();

        ImageIO.write(resizedImage, "png", target.toFile());
    }

    /**
     * 生成数据集描述文件
     */
    private void generateDatasetInfo(Path datasetPath, String datasetName, String description,
                                     Map<Integer, Integer> distribution, int totalImages,
                                     Long operatorId) throws IOException {
        StringBuilder info = new StringBuilder();
        info.append("# 训练集信息\n\n");
        info.append("数据集名称: ").append(datasetName).append("\n");
        info.append("描述: ").append(description != null ? description : "用户反馈生成的训练集").append("\n");
        info.append("生成时间: ").append(LocalDateTime.now()).append("\n");
        info.append("操作人ID: ").append(operatorId).append("\n");
        info.append("图片总数: ").append(totalImages).append("\n\n");
        info.append("## 各类别分布\n\n");
        info.append("| 数字 | 图片数量 |\n");
        info.append("|------|----------|\n");
        for (int i = 0; i <= 9; i++) {
            info.append("| ").append(i).append(" | ")
                    .append(distribution.getOrDefault(i, 0)).append(" |\n");
        }

        Files.writeString(datasetPath.resolve("README.md"), info.toString());
    }

    /**
     * 创建ZIP压缩文件
     */
    private void createZipFile(Path sourceDir, Path zipFile) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(zipFile)))) {

            Files.walk(sourceDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            String entryName = sourceDir.relativize(path).toString();
                            zos.putNextEntry(new ZipEntry(entryName));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            log.error("添加文件到ZIP失败: {}", path, e);
                        }
                    });
        }
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.error("删除文件失败: {}", path, e);
                        }
                    });
        }
    }
}