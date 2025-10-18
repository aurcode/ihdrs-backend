// ModelResponse.java - 模型响应
package com.ihdrs.backend.dto.response;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ModelResponse {

    private Long modelId;
    private String modelName;
    private String modelVersion;
    private String modelType;
    private BigDecimal accuracy;
    private BigDecimal loss;
    private Integer trainingSamples;
    private Integer testSamples;
    private Long modelSize;
    private String status;
    private String description;
    private String creatorName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Boolean isActive; // 是否为当前活跃模型
}