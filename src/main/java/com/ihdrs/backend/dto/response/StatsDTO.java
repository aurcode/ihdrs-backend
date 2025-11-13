package com.ihdrs.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StatsDTO {
    private Long totalRecognitions;
    private Long successRecognitions;
    private Long failedRecognitions;
    private Double successRate;
    private Double errorRate;
    private Double avgProcessingTime;
    private LocalDateTime statsTime;
}