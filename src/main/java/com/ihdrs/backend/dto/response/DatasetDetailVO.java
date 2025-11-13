// DatasetDetailVO.java

package com.ihdrs.backend.dto.response;

import com.ihdrs.backend.entity.Dataset;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetDetailVO {

    private Long datasetId;
    private String datasetName;
    private String datasetType;
    private String description;
    private Long fileSize;
    private String fileSizeFormatted;
    private Integer numClasses;
    private Integer numSamples;
    private Integer trainSamples;
    private Integer testSamples;
    private Integer imageWidth;
    private Integer imageHeight;
    private List<String> classNames;
    private String status;
    private String errorMessage;
    private Boolean isPublic;
    private Long creatorId;
    private String creatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer usageCount; // 使用次数

    public static DatasetDetailVO fromEntity(Dataset dataset) {
        return DatasetDetailVO.builder()
                .datasetId(dataset.getDatasetId())
                .datasetName(dataset.getDatasetName())
                .datasetType(dataset.getDatasetType().name())
                .description(dataset.getDescription())
                .fileSize(dataset.getFileSize())
                .fileSizeFormatted(formatFileSize(dataset.getFileSize()))
                .numClasses(dataset.getNumClasses())
                .numSamples(dataset.getNumSamples())
                .trainSamples(dataset.getTrainSamples())
                .testSamples(dataset.getTestSamples())
                .imageWidth(dataset.getImageWidth())
                .imageHeight(dataset.getImageHeight())
                .status(dataset.getStatus().name())
                .errorMessage(dataset.getErrorMessage())
                .isPublic(dataset.getIsPublic())
                .creatorId(dataset.getCreatorId())
                .createTime(dataset.getCreateTime())
                .updateTime(dataset.getUpdateTime())
                .build();
    }

    private static String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", size, units[unitIndex]);
    }
}