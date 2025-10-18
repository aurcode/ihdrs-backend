// FeedbackService.java - 反馈服务
package com.ihdrs.backend.service;

import com.ihdrs.backend.common.PageResult;
import com.ihdrs.backend.common.Result;
import com.ihdrs.backend.dto.request.FeedbackRequest;
import com.ihdrs.backend.dto.request.PageRequest;
import com.ihdrs.backend.entity.FeedbackData;
import com.ihdrs.backend.entity.RecognitionRecord;
import com.ihdrs.backend.repository.FeedbackDataRepository;
import com.ihdrs.backend.repository.RecognitionRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackDataRepository feedbackRepository;
    private final RecognitionRecordRepository recordRepository;

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
        if (!feedbackRepository.findByRecordId(request.getRecordId()).isEmpty()) {
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
    public Result<PageResult<FeedbackData>> getFeedbackList(PageRequest pageRequest,
                                                            FeedbackData.FeedbackStatus status) {
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(
                        pageRequest.getCurrent().intValue() - 1,
                        pageRequest.getSize().intValue(),
                        Sort.by(Sort.Direction.DESC, "createTime")
                );

        Page<FeedbackData> feedbackPage = status != null ?
                feedbackRepository.findByStatusOrderByCreateTimeDesc(status, springPageRequest) :
                feedbackRepository.findAll(springPageRequest);

        PageResult<FeedbackData> result = PageResult.of(
                feedbackPage.getContent(),
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

        feedback.setStatus(status);
        feedback.setReviewNote(reviewNote);
        feedback.setReviewerId(reviewerId);
        feedback.setReviewTime(java.time.LocalDateTime.now());

        feedbackRepository.save(feedback);

        log.info("反馈审核完成: feedbackId={}, status={}, reviewerId={}",
                feedbackId, status, reviewerId);

        return Result.success("审核完成", null);
    }
}