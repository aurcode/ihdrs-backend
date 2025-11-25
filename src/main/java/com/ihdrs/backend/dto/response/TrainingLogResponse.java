// TrainingLogResponse.java

package com.ihdrs.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 训练日志响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingLogResponse {

    /**
     * 日志ID
     */
    private Long logId;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 训练轮次
     */
    private Integer epoch;

    /**
     * 批次步骤
     */
    private Integer step;

    /**
     * 训练损失
     */
    private BigDecimal loss;

    /**
     * 训练准确率
     */
    private BigDecimal accuracy;

    /**
     * 验证损失
     */
    private BigDecimal valLoss;

    /**
     * 验证准确率
     */
    private BigDecimal valAccuracy;

    /**
     * 学习率
     */
    private BigDecimal learningRate;

    /**
     * 批次大小
     */
    private Integer batchSize;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 额外信息
     */
    private String message;
}
