// ModelVersionResponse.java - 模型版本响应
package com.ihdrs.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelVersionResponse {

    private Long modelId;
    private String modelName;
    private String modelVersion;
    private String modelType;
    private BigDecimal accuracy;
    private BigDecimal loss;
    private Integer trainingSamples;
    private String status;
    private LocalDateTime createTime;
    private Boolean isActive;
    private String creatorName;

    // 版本对比字段
    private BigDecimal accuracyImprovement; // 与上一版本相比的准确率提升
    private String changeDescription; // 版本变更说明
}