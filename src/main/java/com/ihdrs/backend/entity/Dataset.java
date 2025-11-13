// Dataset.java

package com.ihdrs.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "datasets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dataset_id")
    private Long datasetId;

    @Column(name = "dataset_name", nullable = false, length = 100)
    private String datasetName;

    @Enumerated(EnumType.STRING)
    @Column(name = "dataset_type", nullable = false)
    private DatasetType datasetType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "num_classes")
    private Integer numClasses;

    @Column(name = "num_samples")
    private Integer numSamples;

    @Column(name = "train_samples")
    private Integer trainSamples;

    @Column(name = "test_samples")
    private Integer testSamples;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "class_names", columnDefinition = "JSON")
    private String classNames; // 存储为JSON字符串

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DatasetStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    // 枚举类型
    public enum DatasetType {
        IMAGE_CLASSIFICATION,
        OBJECT_DETECTION,
        OTHER
    }

    public enum DatasetStatus {
        UPLOADING,
        PROCESSING,
        AVAILABLE,
        ERROR
    }
}