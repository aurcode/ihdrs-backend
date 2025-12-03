package com.ihdrs.backend.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TrainingDatasetRequest {

    private String datasetName;           // 训练集名称
    private String description;           // 描述
    private LocalDateTime startTime;      // 反馈时间范围-开始
    private LocalDateTime endTime;        // 反馈时间范围-结束
    private Double minQualityScore;       // 最低质量评分筛选
    private Boolean resizeToMNIST;        // 是否调整为28x28 MNIST格式
    private String exportFormat;          // 导出格式: folder, zip
}