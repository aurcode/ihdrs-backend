// TrainingTask.java - 训练任务实体类
package com.ihdrs.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "training_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    private Long taskId;

    @NotBlank(message = "任务名称不能为空")
    @Column(name = "task_name", nullable = false, length = 100)
    private String taskName;

    @NotNull(message = "创建者ID不能为空")
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "model_id")
    private Long modelId;

    @Column(name = "dataset_config", nullable = false, columnDefinition = "JSON")
    private String datasetConfig;

    @Column(name = "training_config", nullable = false, columnDefinition = "JSON")
    private String trainingConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TaskStatus status = TaskStatus.PENDING;

    @Column(name = "progress", precision = 5, scale = 2)
    private BigDecimal progress = BigDecimal.ZERO;

    @Column(name = "current_epoch")
    private Integer currentEpoch = 0;

    @Min(value = 1, message = "训练轮数至少为1")
    @Column(name = "total_epochs", nullable = false)
    private Integer totalEpochs;

    @Column(name = "best_accuracy", precision = 5, scale = 4)
    private BigDecimal bestAccuracy;

    @Column(name = "final_accuracy", precision = 5, scale = 4)
    private BigDecimal finalAccuracy;

    @Column(name = "final_loss", precision = 10, scale = 6)
    private BigDecimal finalLoss;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "estimated_time")
    private Integer estimatedTime;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 外键关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", insertable = false, updatable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", insertable = false, updatable = false)
    private Model model;

    public enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
    }
}