// ModelStatisticsResponse.java

package com.ihdrs.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelStatisticsResponse {

    private Long totalModels;
    private Long activeModels;
    private Long completedModels;
    private Long trainingModels;
    private BigDecimal avgAccuracy;
    private BigDecimal bestAccuracy;
    private Long totalRecognitions;
    private Map<String, Long> modelTypeDistribution;
    private List<TopModel> topModels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopModel {
        private Long modelId;
        private String modelName;
        private String modelVersion;
        private BigDecimal accuracy;
        private Long usageCount;
    }
}