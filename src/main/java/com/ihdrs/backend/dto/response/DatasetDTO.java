// DatasetDTO.java

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
public class DatasetDTO {

    private Long datasetId;
    private String datasetName;
    private String datasetType;
    private String description;
    private Long fileSize;
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

    // 从Entity转换为DTO
    public static DatasetDTO fromEntity(Dataset dataset) {
        return DatasetDTO.builder()
                .datasetId(dataset.getDatasetId())
                .datasetName(dataset.getDatasetName())
                .datasetType(dataset.getDatasetType().name())
                .description(dataset.getDescription())
                .fileSize(dataset.getFileSize())
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
}