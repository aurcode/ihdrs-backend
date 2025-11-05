// FeedbackController.java - 反馈控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.BatchReviewRequest;
import com.ihdrs.backend.dto.request.FeedbackRequest;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.response.FeedbackResponse;
import com.ihdrs.backend.entity.FeedbackData;
import com.ihdrs.backend.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Tag(name = "用户反馈", description = "识别错误反馈相关接口")
@RestController
@RequestMapping("/feedback")
@RequiredArgsConstructor
@Validated
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "提交反馈", description = "用户提交识别错误反馈")
    @PostMapping
    public Result<Void> submitFeedback(
            @Valid @RequestBody FeedbackRequest request,
            @RequestAttribute("userId") Long userId) {
        return feedbackService.submitFeedback(request, userId);
    }

    @Operation(summary = "获取反馈列表", description = "管理员查看反馈列表")
    @GetMapping("/list")
    public Result<PageResult<FeedbackResponse>> getFeedbackList(
            @Valid PageRequest pageRequest,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feedbackType){
        FeedbackData.FeedbackStatus feedbackStatus = null;
        FeedbackData.FeedbackType type = null;

        if (StringUtils.hasText(status)) {
            feedbackStatus = FeedbackData.FeedbackStatus.valueOf(status.trim());
        }

        if (StringUtils.hasText(feedbackType)) {
            type = FeedbackData.FeedbackType.valueOf(feedbackType.trim());
        }

        return feedbackService.getFeedbackList(pageRequest, feedbackStatus, type);
    }

    @Operation(summary = "审核反馈", description = "管理员审核用户反馈")
    @PutMapping("/{feedbackId}/review")
    public Result<Void> reviewFeedback(
            @PathVariable Long feedbackId,
            @RequestParam String status,
            @RequestParam(required = false) String reviewNote,
            @RequestAttribute("userId") Long reviewerId) {
        FeedbackData.FeedbackStatus feedbackStatus = FeedbackData.FeedbackStatus.valueOf(status);
        return feedbackService.reviewFeedback(feedbackId, feedbackStatus, reviewNote, reviewerId);
    }

    /**
     * 批量审核反馈
     */
    @PutMapping("/batch-review")
    @Operation(summary = "批量审核反馈")
    public Result<Void> batchReviewFeedback(@RequestBody BatchReviewRequest request,
                                            @RequestAttribute("userId") Long reviewerId) {
        return feedbackService.batchReviewFeedback(request.getFeedbackIds(),
                request.getStatus(),
                request.getReviewNote(),
                reviewerId);
    }

}