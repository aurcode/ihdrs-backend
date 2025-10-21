// StatisticsResponse.java - 统计响应
package com.ihdrs.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    // 总体统计
    private Long totalRecognitions;
    private Long totalUsers;
    private Long totalModels;
    private Long totalTrainingTasks;

    // 识别统计
    private BigDecimal overallAccuracy;
    private BigDecimal avgConfidence;
    private Integer avgProcessingTime;
    private Long todayRecognitions;

    // 趋势数据
    private List<TrendData> recognitionTrend; // 识别趋势
    private List<TrendData> accuracyTrend; // 准确率趋势

    // 分布数据
    private Map<Integer, Long> digitDistribution; // 数字分布
    private Map<String, Long> inputTypeDistribution; // 输入类型分布

    // 性能数据
    private Integer currentQPS; // 当前QPS
    private BigDecimal errorRate; // 错误率
    private Integer activeUsers; // 活跃用户数

    @Data
    @Builder
    public static class TrendData {
        private String date;
        private Long value;
        private BigDecimal rate;
    }
}