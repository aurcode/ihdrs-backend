// RecognitionResponse.java - 识别响应
package com.ihdrs.backend.dto.response;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;

@Data
@Builder
public class RecognitionResponse {

    private Long recordId;
    private Integer recognitionResult;
    private BigDecimal confidence;
    private Integer processingTime; // 处理时间（毫秒）
    private String message; // 提示信息
    private Boolean needRewrite; // 是否需要重写（置信度低时）
}