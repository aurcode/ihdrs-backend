// DatasetServiceImpl.java

package com.ihdrs.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.dto.response.DatasetDTO;
import com.ihdrs.backend.dto.response.DatasetDetailVO;
import com.ihdrs.backend.dto.request.DatasetUploadRequest;
import com.ihdrs.backend.entity.Dataset;
import com.ihdrs.backend.repository.DatasetRepository;
import com.ihdrs.backend.service.DatasetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    private final DatasetRepository datasetRepository;
    private final ObjectMapper objectMapper;

    @Value("${file.upload.path}")
    private String uploadPath;

    private static final long MAX_DATASET_SIZE = 500 * 1024 * 1024; // 500MB
    private static final String[] ALLOWED_EXTENSIONS = {".zip"};
    private static final String[] ALLOWED_IMAGE_FORMATS = {".jpg", ".jpeg", ".png", ".bmp"};

    @Override
    @Transactional
    public DatasetDTO uploadDataset(MultipartFile file, DatasetUploadRequest request, Long userId) {
        log.info("开始上传数据集: {}, 用户ID: {}", request.getDatasetName(), userId);

        // 验证文件
        validateFile(file);

        // 检查数据集名称是否重复
        datasetRepository.findByDatasetNameAndCreatorId(request.getDatasetName(), userId)
                .ifPresent(d -> {
                    throw new IllegalArgumentException("数据集名称已存在: " + request.getDatasetName());
                });

        try {
            // 创建数据集目录
            String datasetDir = createDatasetDirectory(userId, request.getDatasetName());

            // 保存上传的文件
            String zipFilePath = saveUploadedFile(file, datasetDir);

            // 创建数据集记录
            Dataset dataset = Dataset.builder()
                    .datasetName(request.getDatasetName())
                    .datasetType(Dataset.DatasetType.valueOf(request.getDatasetType()))
                    .description(request.getDescription())
                    .filePath(zipFilePath)
                    .fileSize(file.getSize())
                    .status(Dataset.DatasetStatus.UPLOADING)
                    .isPublic(request.getIsPublic())
                    .creatorId(userId)
                    .build();

            dataset = datasetRepository.save(dataset);
            log.info("数据集记录创建成功，ID: {}", dataset.getDatasetId());

            // 异步处理数据集
            processDatasetAsync(dataset.getDatasetId());

            return DatasetDTO.fromEntity(dataset);

        } catch (Exception e) {
            log.error("上传数据集失败", e);
            throw new RuntimeException("上传数据集失败: " + e.getMessage());
        }
    }

    @Override
    public DatasetDetailVO getDatasetDetail(Long datasetId, Long userId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("数据集不存在"));

        // 检查权限
        if (!dataset.getCreatorId().equals(userId) && !dataset.getIsPublic()) {
            throw new IllegalArgumentException("无权限访问该数据集");
        }

        DatasetDetailVO detailVO = DatasetDetailVO.fromEntity(dataset);

        // 解析类别名称
        if (dataset.getClassNames() != null) {
            try {
                List<String> classNames = objectMapper.readValue(
                        dataset.getClassNames(),
                        new TypeReference<List<String>>() {}
                );
                detailVO.setClassNames(classNames);
            } catch (Exception e) {
                log.error("解析类别名称失败", e);
            }
        }

        return detailVO;
    }

    @Override
    public PageResult<DatasetDTO> getUserDatasets(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Dataset> datasetPage = datasetRepository.findByCreatorId(userId, pageable);

        List<DatasetDTO> dtoList = datasetPage.getContent().stream()
                .map(DatasetDTO::fromEntity)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, datasetPage.getTotalElements(),
                (long) size, (long) page);
    }

    @Override
    public List<DatasetDTO> getAvailableDatasets(Long userId) {
        List<Dataset> datasets = datasetRepository.findAvailableDatasets(userId);
        return datasets.stream()
                .map(DatasetDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<DatasetDTO> getPublicDatasets(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Dataset> datasetPage = datasetRepository.findByIsPublicTrue(pageable);

        List<DatasetDTO> dtoList = datasetPage.getContent().stream()
                .map(DatasetDTO::fromEntity)
                .collect(Collectors.toList());

        return PageResult.of(dtoList, datasetPage.getTotalElements(),
                (long) size, (long) page);
    }

    @Override
    @Transactional
    public void deleteDataset(Long datasetId, Long userId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("数据集不存在"));

        // 检查权限
        if (!dataset.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("无权限删除该数据集");
        }

        // 删除文件
        try {
            Path datasetPath = Paths.get(dataset.getFilePath()).getParent();
            deleteDirectory(datasetPath);
            log.info("已删除数据集文件: {}", datasetPath);
        } catch (IOException e) {
            log.error("删除数据集文件失败", e);
        }

        // 删除数据库记录
        datasetRepository.delete(dataset);
        log.info("已删除数据集记录，ID: {}", datasetId);
    }

    @Override
    @Transactional
    public DatasetDTO updateDataset(Long datasetId, DatasetUploadRequest request, Long userId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("数据集不存在"));

        // 检查权限
        if (!dataset.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("无权限修改该数据集");
        }

        // 检查名称是否重复
        if (!dataset.getDatasetName().equals(request.getDatasetName())) {
            datasetRepository.findByDatasetNameAndCreatorId(request.getDatasetName(), userId)
                    .ifPresent(d -> {
                        throw new IllegalArgumentException("数据集名称已存在: " + request.getDatasetName());
                    });
        }

        // 更新数据集信息
        dataset.setDatasetName(request.getDatasetName());
        dataset.setDescription(request.getDescription());
        dataset.setIsPublic(request.getIsPublic());

        dataset = datasetRepository.save(dataset);
        return DatasetDTO.fromEntity(dataset);
    }

    @Override
    @Transactional
    public void processDataset(Long datasetId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("数据集不存在"));

        try {
            log.info("开始处理数据集，ID: {}", datasetId);
            dataset.setStatus(Dataset.DatasetStatus.PROCESSING);
            datasetRepository.save(dataset);

            // 解压数据集
            String extractDir = extractZipFile(dataset.getFilePath());

            // 验证和分析数据集
            DatasetInfo info = analyzeDataset(extractDir);

            dataset.setFilePath(extractDir.replace("\\", "/"));

            // 更新数据集信息
            dataset.setNumClasses(info.numClasses);
            dataset.setNumSamples(info.numSamples);
            dataset.setTrainSamples(info.trainSamples);
            dataset.setTestSamples(info.testSamples);
            dataset.setImageWidth(info.imageWidth);
            dataset.setImageHeight(info.imageHeight);
            dataset.setClassNames(objectMapper.writeValueAsString(info.classNames));
            dataset.setStatus(Dataset.DatasetStatus.AVAILABLE);

            datasetRepository.save(dataset);
            log.info("数据集处理完成，ID: {}", datasetId);

        } catch (Exception e) {
            log.error("处理数据集失败，ID: " + datasetId, e);
            dataset.setStatus(Dataset.DatasetStatus.ERROR);
            dataset.setErrorMessage(e.getMessage());
            datasetRepository.save(dataset);
        }
    }

    @Override
    @Transactional
    public void setDatasetPublic(Long datasetId, Boolean isPublic, Long userId) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("数据集不存在"));

        if (!dataset.getCreatorId().equals(userId)) {
            throw new IllegalArgumentException("无权限修改该数据集");
        }

        dataset.setIsPublic(isPublic);
        datasetRepository.save(dataset);
        log.info("数据集公开状态已更新，ID: {}, 公开: {}", datasetId, isPublic);
    }

    // ========== 私有辅助方法 ==========

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        if (file.getSize() > MAX_DATASET_SIZE) {
            throw new IllegalArgumentException("文件大小超过限制（最大500MB）");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("只支持ZIP格式的压缩文件");
        }
    }

    private String createDatasetDirectory(Long userId, String datasetName) throws IOException {
        String sanitizedName = datasetName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String dirName = String.format("dataset_%d_%s_%s", userId, sanitizedName, timestamp);

        Path dirPath = Paths.get(uploadPath, "datasets", dirName);
        Files.createDirectories(dirPath);

        return dirPath.toString();
    }

    private String saveUploadedFile(MultipartFile file, String datasetDir) throws IOException {
        String filename = "dataset.zip";
        Path filePath = Paths.get(datasetDir, filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }

    private String extractZipFile(String zipFilePath) throws IOException {
        Path zipPath = Paths.get(zipFilePath);
        Path extractDir = zipPath.getParent().resolve("extracted");
        Files.createDirectories(extractDir);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = extractDir.resolve(entry.getName());

                // 安全检查：防止路径遍历攻击
                if (!filePath.normalize().startsWith(extractDir.normalize())) {
                    throw new IOException("非法的压缩文件路径: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }

        return extractDir.toString();
    }

    private DatasetInfo analyzeDataset(String datasetDir) throws IOException {
        DatasetInfo info = new DatasetInfo();
        Path rootPath = Paths.get(datasetDir);

        // 查找训练集和测试集目录
        Path trainDir = findDirectory(rootPath, "train");
        Path testDir = findDirectory(rootPath, "test");

        if (trainDir == null) {
            throw new IOException("未找到训练集目录（train）");
        }

        // 分析训练集
        Map<String, Integer> trainClassCounts = analyzeClassDirectory(trainDir);
        info.classNames = new ArrayList<>(trainClassCounts.keySet());
        Collections.sort(info.classNames);
        info.numClasses = info.classNames.size();
        info.trainSamples = trainClassCounts.values().stream().mapToInt(Integer::intValue).sum();

        // 分析测试集（如果存在）
        if (testDir != null) {
            Map<String, Integer> testClassCounts = analyzeClassDirectory(testDir);
            info.testSamples = testClassCounts.values().stream().mapToInt(Integer::intValue).sum();
        }

        info.numSamples = info.trainSamples + info.testSamples;

        // 获取图像尺寸（从第一张图片）
        BufferedImage sampleImage = findFirstImage(trainDir);
        if (sampleImage != null) {
            info.imageWidth = sampleImage.getWidth();
            info.imageHeight = sampleImage.getHeight();
        }

        return info;
    }

    private Path findDirectory(Path rootPath, String dirName) throws IOException {
        try (var stream = Files.walk(rootPath, 3)) {
            return stream
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(dirName))
                    .findFirst()
                    .orElse(null);
        }
    }

    private Map<String, Integer> analyzeClassDirectory(Path classRootDir) throws IOException {
        Map<String, Integer> classCounts = new HashMap<>();

        try (var stream = Files.list(classRootDir)) {
            List<Path> classDirs = stream
                    .filter(Files::isDirectory)
                    .collect(Collectors.toList());

            for (Path classDir : classDirs) {
                String className = classDir.getFileName().toString();
                int imageCount = countImages(classDir);
                if (imageCount > 0) {
                    classCounts.put(className, imageCount);
                }
            }
        }

        if (classCounts.isEmpty()) {
            throw new IOException("数据集目录结构不正确，请确保按类别组织图像");
        }

        return classCounts;
    }

    private int countImages(Path directory) throws IOException {
        try (var stream = Files.walk(directory)) {
            return (int) stream
                    .filter(Files::isRegularFile)
                    .filter(this::isImageFile)
                    .count();
        }
    }

    private boolean isImageFile(Path file) {
        String filename = file.getFileName().toString().toLowerCase();
        return Arrays.stream(ALLOWED_IMAGE_FORMATS)
                .anyMatch(filename::endsWith);
    }

    private BufferedImage findFirstImage(Path directory) throws IOException {
        try (var stream = Files.walk(directory)) {
            Optional<Path> firstImage = stream
                    .filter(Files::isRegularFile)
                    .filter(this::isImageFile)
                    .findFirst();

            if (firstImage.isPresent()) {
                return ImageIO.read(firstImage.get().toFile());
            }
        }
        return null;
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (var stream = Files.walk(directory)) {
                stream.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    private void processDatasetAsync(Long datasetId) {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 短暂延迟以确保事务提交
                processDataset(datasetId);
            } catch (Exception e) {
                log.error("异步处理数据集失败", e);
            }
        }).start();
    }

    // 数据集信息内部类
    private static class DatasetInfo {
        Integer numClasses;
        Integer numSamples;
        Integer trainSamples = 0;
        Integer testSamples = 0;
        Integer imageWidth;
        Integer imageHeight;
        List<String> classNames;
    }
}