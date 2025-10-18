// RecognitionRecord.java - 识别记录实体类
package com.ihdrs.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "recognition_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecognitionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "user_id")
    private Long userId;

    @NotNull(message = "模型ID不能为空")
    @Column(name = "model_id", nullable = false)
    private Long modelId;

    @Min(value = 0, message = "识别结果必须在0-9之间")
    @Max(value = 9, message = "识别结果必须在0-9之间")
    @Column(name = "recognition_result", nullable = false)
    private Integer recognitionResult;

    @DecimalMin(value = "0.0", message = "置信度不能小于0")
    @DecimalMax(value = "1.0", message = "置信度不能大于1")
    @Column(name = "confidence", nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Lob
    @Column(name = "image_data")
    private byte[] imageData;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "image_hash", length = 64)
    private String imageHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type")
    private InputType inputType = InputType.CANVAS;

    @Column(name = "processing_time")
    private Integer processingTime;

    @Column(name = "client_info", columnDefinition = "JSON")
    private String clientInfo;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    // 外键关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", insertable = false, updatable = false)
    private Model model;

    public enum InputType {
        CANVAS, UPLOAD, CAMERA
    }
}