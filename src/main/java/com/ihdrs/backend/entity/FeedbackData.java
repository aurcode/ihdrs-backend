// FeedbackData.java - 反馈数据实体类
package com.ihdrs.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @NotNull(message = "识别记录ID不能为空")
    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @NotNull(message = "用户ID不能为空")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Min(value = 0, message = "原始结果必须在0-9之间")
    @Max(value = 9, message = "原始结果必须在0-9之间")
    @Column(name = "original_result", nullable = false)
    private Integer originalResult;

    @Min(value = 0, message = "正确结果必须在0-9之间")
    @Max(value = 9, message = "正确结果必须在0-9之间")
    @Column(name = "correct_result", nullable = false)
    private Integer correctResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type")
    private FeedbackType feedbackType = FeedbackType.WRONG_RESULT;

    @Column(name = "feedback_reason", length = 500)
    private String feedbackReason;

    @Min(value = 1, message = "质量评分必须在1-5之间")
    @Max(value = 5, message = "质量评分必须在1-5之间")
    @Column(name = "quality_score")
    private Integer qualityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private FeedbackStatus status = FeedbackStatus.PENDING;

    @Column(name = "reviewer_id")
    private Long reviewerId;

    @Column(name = "review_time")
    private LocalDateTime reviewTime;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @CreationTimestamp
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    // 外键关联
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", insertable = false, updatable = false)
    private RecognitionRecord recognitionRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", insertable = false, updatable = false)
    private User reviewer;

    public enum FeedbackType {
        WRONG_RESULT, LOW_CONFIDENCE, OTHER
    }

    public enum FeedbackStatus {
        PENDING, REVIEWED, ACCEPTED, REJECTED
    }
}