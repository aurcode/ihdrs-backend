// Model.java - 模型实体类
package com.ihdrs.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "models")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Model {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Long modelId;

    @NotBlank(message = "模型名称不能为空")
    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @NotBlank(message = "模型版本不能为空")
    @Column(name = "model_version", nullable = false, length = 20)
    private String modelVersion;

    @NotBlank(message = "模型路径不能为空")
    @Column(name = "model_path", nullable = false, length = 500)
    private String modelPath;

    @Column(name = "model_type", length = 50)
    private String modelType = "CNN";

    @DecimalMin(value = "0.0", message = "准确率不能小于0")
    @DecimalMax(value = "1.0", message = "准确率不能大于1")
    @Column(name = "accuracy", precision = 5, scale = 4)
    private BigDecimal accuracy;

    @Column(name = "loss", precision = 10, scale = 6)
    private BigDecimal loss;

    @Column(name = "training_samples")
    private Integer trainingSamples;

    @Column(name = "test_samples")
    private Integer testSamples;

    @Column(name = "model_size")
    private Long modelSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ModelStatus status = ModelStatus.TRAINING;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "hyperparameters", columnDefinition = "JSON")
    private String hyperparameters;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 外键关联，多对一关系，懒加载，不会被用于插入或更新 SQL
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", insertable = false, updatable = false)
    private User creator;

    public enum ModelStatus {
        TRAINING, COMPLETED, ACTIVE, DISABLED
    }
}