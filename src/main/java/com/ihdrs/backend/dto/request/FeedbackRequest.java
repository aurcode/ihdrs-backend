// FeedbackRequest.java - 反馈请求
package com.ihdrs.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class FeedbackRequest {

    @NotNull(message = "识别记录ID不能为空")
    private Long recordId;

    @NotNull(message = "正确结果不能为空")
    private Integer correctResult;

    private String feedbackType = "WRONG_RESULT"; // WRONG_RESULT, LOW_CONFIDENCE, OTHER

    private String feedbackReason; // 反馈原因

    @Min(value = 1, message = "质量评分最低为1")
    @Max(value = 5, message = "质量评分最高为5")
    private Integer qualityScore;
}