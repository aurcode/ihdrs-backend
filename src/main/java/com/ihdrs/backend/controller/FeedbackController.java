// FeedbackController.java - 反馈控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.FeedbackRequest;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.entity.FeedbackData;
import com.ihdrs.backend.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
    public Result<PageResult<FeedbackData>> getFeedbackList(
            @Valid PageRequest pageRequest,
            @RequestParam(required = false) String status) {
        FeedbackData.FeedbackStatus feedbackStatus = null;
        if (status != null) {
            feedbackStatus = FeedbackData.FeedbackStatus.valueOf(status);
        }
        return feedbackService.getFeedbackList(pageRequest, feedbackStatus);
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
}