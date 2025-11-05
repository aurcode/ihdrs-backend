package com.ihdrs.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchReviewRequest {

    @NotEmpty(message = "反馈ID列表不能为空")
    private List<Long> feedbackIds;

    @NotNull(message = "审核状态不能为空")
    private String status;

    private String reviewNote;
}
