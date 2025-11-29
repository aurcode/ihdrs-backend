// FeedbackController.java - 反馈控制器
package com.ihdrs.backend.controller;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.BatchReviewRequest;
import com.ihdrs.backend.dto.request.FeedbackRequest;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.response.FeedbackResponse;
import com.ihdrs.backend.entity.FeedbackData;
import com.ihdrs.backend.service.ExportService;
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
    private final ExportService exportService;

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

    @Operation(summary = "获取当前用户的反馈列表", description = "用户查看自己提交的反馈")
    @GetMapping("/my-feedback")
    public Result<PageResult<FeedbackResponse>> getMyFeedbackList(
            @Valid PageRequest pageRequest,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feedbackType,
            @RequestAttribute("userId") Long userId) {

        FeedbackData.FeedbackStatus feedbackStatus = null;
        FeedbackData.FeedbackType type = null;

        if (StringUtils.hasText(status)) {
            feedbackStatus = FeedbackData.FeedbackStatus.valueOf(status.trim());
        }

        if (StringUtils.hasText(feedbackType)) {
            type = FeedbackData.FeedbackType.valueOf(feedbackType.trim());
        }

        return feedbackService.getUserFeedbackList(userId, pageRequest, feedbackStatus, type);
    }

    @Operation(summary = "获取反馈详情", description = "查看反馈详细信息")
    @GetMapping("/{feedbackId}")
    public Result<FeedbackResponse> getFeedbackById(@PathVariable Long feedbackId) {
        return feedbackService.getFeedbackById(feedbackId);
    }

    @Operation(summary = "删除反馈", description = "用户删除自己的反馈（仅待审核状态）")
    @DeleteMapping("/{feedbackId}")
    public Result<Void> deleteFeedback(
            @PathVariable Long feedbackId,
            @RequestAttribute("userId") Long userId) {
        return feedbackService.deleteFeedback(feedbackId, userId);
    }

    @Operation(summary = "导出反馈数据", description = "导出反馈数据报表，支持Excel、CSV、PDF格式")
    @GetMapping("/export")
    public void exportFeedback(
            HttpServletResponse response,
            @RequestParam(defaultValue = "excel") String format,
            @RequestParam(defaultValue = "filtered") String scope,
            @RequestParam(required = false) String fields,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String feedbackType) throws IOException {

        FeedbackData.FeedbackStatus feedbackStatus = null;
        FeedbackData.FeedbackType type = null;

        try {
            if (StringUtils.hasText(status)) {
                feedbackStatus = FeedbackData.FeedbackStatus.valueOf(status.trim());
            }
            if (StringUtils.hasText(feedbackType)) {
                type = FeedbackData.FeedbackType.valueOf(feedbackType.trim());
            }
        } catch (IllegalArgumentException e) {
            response.setStatus(400);
            response.getWriter().write("无效的状态或反馈类型参数");
            return;
        }

        exportService.exportFeedback(response, format, scope, fields, current, size, feedbackStatus, type);
    }
}