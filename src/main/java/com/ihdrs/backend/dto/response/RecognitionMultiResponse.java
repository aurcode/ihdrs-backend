package com.ihdrs.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RecognitionMultiResponse {
    private Long recordId;
    private String sequence;
    private Integer count;
    private Integer processingTime;
    private List<Map<String, Object>> results;
    private String message;
    private Boolean needRewrite;
}
