// FeedbackService.java
package com.ihdrs.backend.service;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.FeedbackRequest;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.dto.response.FeedbackResponse;
import com.ihdrs.backend.entity.FeedbackData;
import com.ihdrs.backend.entity.RecognitionRecord;
import com.ihdrs.backend.repository.FeedbackDataRepository;
import com.ihdrs.backend.repository.RecognitionRecordRepository;
import com.ihdrs.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackDataRepository feedbackRepository;
    private final RecognitionRecordRepository recordRepository;
    private final UserRepository userRepository;

    /**
     * 提交用户反馈
     */
    @Transactional
    public Result<Void> submitFeedback(FeedbackRequest request, Long userId) {
        // 验证识别记录是否存在
        RecognitionRecord record = recordRepository.findById(request.getRecordId())
                .orElse(null);

        if (record == null) {
            return Result.error(404, "识别记录不存在");
        }

        // 检查是否已经反馈过
        List<FeedbackData> existingFeedback = feedbackRepository.findByRecordId(request.getRecordId());
        if (!existingFeedback.isEmpty()) {
            return Result.error(400, "该记录已有反馈");
        }

        FeedbackData feedback = new FeedbackData();
        feedback.setRecordId(request.getRecordId());
        feedback.setUserId(userId);
        feedback.setOriginalResult(record.getRecognitionResult());
        feedback.setCorrectResult(request.getCorrectResult());
        feedback.setFeedbackType(FeedbackData.FeedbackType.valueOf(request.getFeedbackType()));
        feedback.setFeedbackReason(request.getFeedbackReason());
        feedback.setQualityScore(request.getQualityScore());
        feedback.setStatus(FeedbackData.FeedbackStatus.PENDING);

        feedbackRepository.save(feedback);

        // 更新识别记录的正确性标记
        record.setIsCorrect(record.getRecognitionResult().equals(request.getCorrectResult()));
        recordRepository.save(record);

        log.info("用户提交反馈: userId={}, recordId={}, correctResult={}",
                userId, request.getRecordId(), request.getCorrectResult());

        return Result.success("反馈提交成功", null);
    }

    /**
     * 分页查询反馈数据
     */
    public Result<PageResult<FeedbackResponse>> getFeedbackList(
            PageRequest pageRequest,
            FeedbackData.FeedbackStatus status,
            FeedbackData.FeedbackType type) {

        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.getCurrent().intValue() - 1,
                        pageRequest.getSize().intValue(),
                        Sort.by(Sort.Direction.DESC, "createTime")
                );

        Page<FeedbackData> feedbackPage;

        if (status != null && type != null) {
            feedbackPage = feedbackRepository.findByFeedbackTypeAndStatusOrderByCreateTimeDesc(
                    type, status, springPageRequest);
        } else if (status != null) {
            feedbackPage = feedbackRepository.findByStatusOrderByCreateTimeDesc(status, springPageRequest);
        } else if (type != null) {
            feedbackPage = feedbackRepository.findByFeedbackTypeOrderByCreateTimeDesc(type, springPageRequest);
        } else {
            feedbackPage = feedbackRepository.findAll(springPageRequest);
        }

        List<FeedbackResponse> feedbackList = feedbackPage.getContent().stream()
                .map(this::convertToFeedbackResponse)
                .collect(Collectors.toList());

        PageResult<FeedbackResponse> result = PageResult.of(
                feedbackList,
                feedbackPage.getTotalElements(),
                pageRequest.getSize(),
                pageRequest.getCurrent()
        );

        return Result.success(result);
    }


    /**
     * 获取用户自己的反馈列表
     */
    public Result<PageResult<FeedbackResponse>> getUserFeedbackList(Long userId, PageRequest pageRequest) {
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.getCurrent().intValue() - 1,
                        pageRequest.getSize().intValue(),
                        Sort.by(Sort.Direction.DESC, "createTime")
                );

        Page<FeedbackData> feedbackPage = feedbackRepository.findByUserIdOrderByCreateTimeDesc(
                userId, springPageRequest);

        List<FeedbackResponse> feedbackList = feedbackPage.getContent().stream()
                .map(this::convertToFeedbackResponse)
                .collect(Collectors.toList());

        PageResult<FeedbackResponse> result = PageResult.of(
                feedbackList,
                feedbackPage.getTotalElements(),
                pageRequest.getSize(),
                pageRequest.getCurrent()
        );

        return Result.success(result);
    }

    /**
     * 审核反馈
     */
    @Transactional
    public Result<Void> reviewFeedback(Long feedbackId, FeedbackData.FeedbackStatus status,
                                       String reviewNote, Long reviewerId) {
        FeedbackData feedback = feedbackRepository.findById(feedbackId).orElse(null);
        if (feedback == null) {
            return Result.error(404, "反馈记录不存在");
        }

        if (feedback.getStatus() != FeedbackData.FeedbackStatus.PENDING) {
            return Result.error(400, "该反馈已被审核");
        }

        feedback.setStatus(status);
        feedback.setReviewNote(reviewNote);
        feedback.setReviewerId(reviewerId);
        feedback.setReviewTime(LocalDateTime.now());

        feedbackRepository.save(feedback);

        log.info("反馈审核完成: feedbackId={}, status={}, reviewerId={}",
                feedbackId, status, reviewerId);

        return Result.success("审核完成", null);
    }

    /**
     * 获取反馈详情
     */
    public Result<FeedbackResponse> getFeedbackById(Long feedbackId) {
        FeedbackData feedback = feedbackRepository.findById(feedbackId).orElse(null);
        if (feedback == null) {
            return Result.error(404, "反馈记录不存在");
        }

        return Result.success(convertToFeedbackResponse(feedback));
    }

    /**
     * 删除反馈
     */
    @Transactional
    public Result<Void> deleteFeedback(Long feedbackId, Long userId) {
        FeedbackData feedback = feedbackRepository.findById(feedbackId).orElse(null);
        if (feedback == null) {
            return Result.error(404, "反馈记录不存在");
        }

        // 验证权限：只能删除自己的反馈
        if (!feedback.getUserId().equals(userId)) {
            return Result.error(403, "无权限删除他人的反馈");
        }

        if (feedback.getStatus() != FeedbackData.FeedbackStatus.PENDING) {
            return Result.error(400, "已审核的反馈无法删除");
        }

        feedbackRepository.deleteById(feedbackId);
        log.info("删除反馈: feedbackId={}, userId={}", feedbackId, userId);

        return Result.success("删除成功", null);
    }

    /**
     * 批量删除识别记录
     */
    @Transactional
    public Result<Map<String, Object>> batchDeleteRecords(List<Long> recordIds, Long userId) {
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        for (Long recordId : recordIds) {
            try {
                RecognitionRecord record = recordRepository.findById(recordId).orElse(null);
                if (record == null) {
                    failCount++;
                    errors.add("记录 " + recordId + " 不存在");
                    continue;
                }

                Long ownerId = record.getUserId();
                if (ownerId == null || !ownerId.equals(userId)) {
                    failCount++;
                    errors.add("记录 " + recordId + " 无权限删除");
                    continue;
                }

                recordRepository.deleteById(recordId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                errors.add("记录 " + recordId + " 删除失败: " + e.getMessage());
                log.error("删除记录失败: recordId={}", recordId, e);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("errors", errors);

        return Result.success(result);
    }

    /**
     * 获取用户识别统计数据
     */
    public Result<Map<String, Object>> getUserStatistics(Long userId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime monthAgo = now.minusMonths(1);

            // 总识别次数
            long totalRecognitions = recordRepository.countByUserId(userId);

            // 最近30天识别次数
            long recentRecognitions = recordRepository.countByUserIdAndCreateTimeBetween(
                    userId, monthAgo, now);

            // 各数字识别次数分布
            List<Object[]> digitDistribution = recordRepository
                    .countByUserIdAndRecognitionResult(userId);

            Map<Integer, Long> digitMap = new HashMap<>();
            for (Object[] row : digitDistribution) {
                digitMap.put((Integer) row[0], (Long) row[1]);
            }

            // 平均置信度
            BigDecimal avgConfidence = recordRepository.avgConfidenceByUserId(userId);

            // 最高置信度和最低置信度
            BigDecimal maxConfidence = recordRepository.maxConfidenceByUserId(userId);
            BigDecimal minConfidence = recordRepository.minConfidenceByUserId(userId);

            Map<String, Object> result = new HashMap<>();
            result.put("totalRecognitions", totalRecognitions);
            result.put("recentRecognitions", recentRecognitions);
            result.put("digitDistribution", digitMap);
            result.put("avgConfidence", avgConfidence);
            result.put("maxConfidence", maxConfidence);
            result.put("minConfidence", minConfidence);

            return Result.success(result);
        } catch (Exception e) {
            log.error("获取用户统计数据失败", e);
            return Result.error(500, "获取统计数据失败");
        }
    }

    /**
     * 批量审核反馈
     */
    @Transactional
    public Result<Void> batchReviewFeedback(List<Long> feedbackIds, String statusStr,
                                            String reviewNote, Long reviewerId) {
        try {
            if (feedbackIds == null || feedbackIds.isEmpty()) {
                return Result.error(400, "反馈ID列表不能为空");
            }

            FeedbackData.FeedbackStatus status = FeedbackData.FeedbackStatus.valueOf(statusStr);
            LocalDateTime reviewTime = LocalDateTime.now();

            feedbackRepository.batchUpdateStatus(feedbackIds, status, reviewerId,
                    reviewTime, reviewNote);

            log.info("批量审核反馈成功 - 审核人ID: {}, 状态: {}, 数量: {}",
                    reviewerId, status, feedbackIds.size());
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            return Result.error(400, "无效的状态值");
        } catch (Exception e) {
            log.error("批量审核反馈失败", e);
            return Result.error(500, "批量审核失败: " + e.getMessage());
        }
    }

    /**
     * 转换为反馈响应对象
     */
    private FeedbackResponse convertToFeedbackResponse(FeedbackData feedback) {
        FeedbackResponse.FeedbackResponseBuilder builder = FeedbackResponse.builder()
                .feedbackId(feedback.getFeedbackId())
                .recordId(feedback.getRecordId())
                .userId(feedback.getUserId())
                .originalResult(feedback.getOriginalResult())
                .correctResult(feedback.getCorrectResult())
                .feedbackType(feedback.getFeedbackType().name())
                .feedbackReason(feedback.getFeedbackReason())
                .qualityScore(feedback.getQualityScore())
                .status(feedback.getStatus().name())
                .reviewerId(feedback.getReviewerId())
                .reviewTime(feedback.getReviewTime())
                .reviewNote(feedback.getReviewNote())
                .createTime(feedback.getCreateTime());

        // 获取用户名
        if (feedback.getUserId() != null) {
            userRepository.findById(feedback.getUserId())
                    .ifPresent(user -> builder.username(user.getUsername()));
        }

        // 获取审核人名
        if (feedback.getReviewerId() != null) {
            userRepository.findById(feedback.getReviewerId())
                    .ifPresent(reviewer -> builder.reviewerName(reviewer.getUsername()));
        }

        // 获取识别记录信息
        if (feedback.getRecordId() != null) {
            recordRepository.findById(feedback.getRecordId())
                    .ifPresent(record -> {
                        FeedbackResponse.RecognitionRecordInfo recordInfo =
                                FeedbackResponse.RecognitionRecordInfo.builder()
                                        .imagePath(record.getImagePath())
                                        .confidence(record.getConfidence().toString())
                                        .recognitionTime(record.getCreateTime())
                                        .build();
                        builder.recordInfo(recordInfo);
                    });
        }

        return builder.build();
    }

}