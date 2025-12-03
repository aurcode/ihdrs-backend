package com.ihdrs.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class TrainingDatasetResponse {

    private Long datasetId;
    private String datasetName;
    private String datasetPath;
    private String downloadUrl;
    private Integer totalImages;
    private Map<Integer, Integer> classDistribution;  // 每个数字的图片数量
    private LocalDateTime createTime;
    private String status;
    private String message;
}