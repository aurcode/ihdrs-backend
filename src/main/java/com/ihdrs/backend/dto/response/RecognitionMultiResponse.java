package com.ihdrs.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RecognitionMultiResponse {
    private Long recordId;           // 新增：记录ID
    private String sequence;         // 新增：完整数字序列（如 "12345"）
    private Integer count;           // 识别到的数字个数
    private Integer processingTime;  // 处理时间
    private List<Map<String, Object>> results; // 每个数字的详细结果
    private String message;          // 新增：识别消息
    private Boolean needRewrite;     // 新增：是否需要重写
}
