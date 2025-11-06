// ModelComparisonResponse.java

package com.ihdrs.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelComparisonResponse {

    private ModelBasicInfo model1;
    private ModelBasicInfo model2;
    private ComparisonMetrics comparison;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelBasicInfo {
        private Long modelId;
        private String modelName;
        private String modelVersion;
        private String modelType;
        private BigDecimal accuracy;
        private BigDecimal loss;
        private Integer trainingSamples;
        private Long modelSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComparisonMetrics {
        private BigDecimal accuracyDiff;
        private BigDecimal lossDiff;
        private Integer samplesDiff;
        private Long sizeDiff;
        private String recommendation; // 推荐哪个模型
        private Map<String, Object> detailedMetrics; // 详细指标对比
    }
}