package com.ihdrs.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecognitionHistoryDTO {
    private Long id;
    private String imageName;
    private String result;
    private String status;
    private String sequenceResult;
    private Double confidence;
    private Long processingTime;
    private LocalDateTime createTime;
    private String errorMessage;
    private String modelName;
    private String userName;
}