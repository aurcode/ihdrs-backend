// TrainingTaskResponse.java - 训练任务响应
package com.ihdrs.backend.dto.response;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TrainingTaskResponse {

    private Long taskId;
    private String taskName;
    private String status;
    private BigDecimal progress;
    private Integer currentEpoch;
    private Integer totalEpochs;
    private BigDecimal bestAccuracy;
    private BigDecimal finalAccuracy;
    private BigDecimal finalLoss;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer estimatedTime;
    private String creatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 生成的模型信息
    private Long modelId;
    private String modelName;
    private String modelVersion;
}