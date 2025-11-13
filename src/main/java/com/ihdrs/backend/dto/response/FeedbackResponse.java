// FeedbackResponse.java
package com.ihdrs.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {

    private Long feedbackId;
    private Long recordId;
    private Long userId;
    private String username;
    private Integer originalResult;
    private Integer correctResult;
    private String feedbackType;
    private String feedbackReason;
    private Integer qualityScore;
    private String status;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private String reviewNote;
    private LocalDateTime createTime;

    // 关联的识别记录信息
    private RecognitionRecordInfo recordInfo;

    // 模型信息字段
    private Long modelId;
    private String modelName;
    private String modelVersion;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecognitionRecordInfo {
        private String imagePath;
        private String confidence;
        private LocalDateTime recognitionTime;

        // 模型信息字段
        private Long modelId;
        private String modelName;
        private String modelVersion;
    }
}