// TrainingLog.java
package com.ihdrs.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "training_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrainingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "epoch", nullable = false)
    private Integer epoch;

    @Column(name = "step")
    private Integer step;

    @Column(name = "loss", nullable = false, precision = 10, scale = 6)
    private BigDecimal loss;

    @Column(name = "accuracy", precision = 5, scale = 4)
    private BigDecimal accuracy;

    @Column(name = "val_loss", precision = 10, scale = 6)
    private BigDecimal valLoss;

    @Column(name = "val_accuracy", precision = 5, scale = 4)
    private BigDecimal valAccuracy;

    @Column(name = "learning_rate", precision = 10, scale = 8)
    private BigDecimal learningRate;

    @Column(name = "batch_size")
    private Integer batchSize;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", insertable = false, updatable = false)
    private TrainingTask task;
}
