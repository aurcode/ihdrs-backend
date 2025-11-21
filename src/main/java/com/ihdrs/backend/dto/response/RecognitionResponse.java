// RecognitionResponse.java - 识别响应
package com.ihdrs.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionResponse {

    private Long userId;
    private Long recordId;
    private Integer recognitionResult;
    private String sequenceResult;
    private BigDecimal confidence;
    private Integer processingTime; // 处理时间（毫秒）
    private String message; // 提示信息
    private Boolean needRewrite; // 是否需要重写（置信度低时）
    private LocalDateTime createTime;
    private String imagePath;
    private String inputType;
    private Boolean isCorrect;
    private List<Double> probabilities;      // 0-9 每一类概率
    private Map<Integer, Double> probabilitiesMap; // 可选：digit -> probability

    // 模型信息字段
    private Long modelId;
    private String modelName;
    private String modelVersion;
}