// FeedbackDataRepository.java - 反馈数据访问
package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.FeedbackData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackDataRepository extends JpaRepository<FeedbackData, Long> {

    /**
     * 根据状态分页查询反馈数据
     */
    Page<FeedbackData> findByStatusOrderByCreateTimeDesc(FeedbackData.FeedbackStatus status, Pageable pageable);

    /**
     * 根据用户ID查询反馈数据
     */
    Page<FeedbackData> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据识别记录ID查询反馈
     */
    List<FeedbackData> findByRecordId(Long recordId);

    @Modifying
    @Query("UPDATE FeedbackData f SET f.status = :status, f.reviewerId = :reviewerId, " +
            "f.reviewTime = :reviewTime, f.reviewNote = :reviewNote " +
            "WHERE f.feedbackId IN :feedbackIds")
    void batchUpdateStatus(@Param("feedbackIds") List<Long> feedbackIds,
                           @Param("status") FeedbackData.FeedbackStatus status,
                           @Param("reviewerId") Long reviewerId,
                           @Param("reviewTime") LocalDateTime reviewTime,
                           @Param("reviewNote") String reviewNote);

    @Query("SELECT COUNT(f) FROM FeedbackData f WHERE f.status = :status")
    Long countByStatus(@Param("status") FeedbackData.FeedbackStatus status);

    Page<FeedbackData> findByFeedbackTypeAndStatusOrderByCreateTimeDesc(
            FeedbackData.FeedbackType feedbackType,
            FeedbackData.FeedbackStatus status,
            Pageable pageable);

    Page<FeedbackData> findByFeedbackTypeOrderByCreateTimeDesc(
            FeedbackData.FeedbackType feedbackType,
            Pageable pageable);

    Page<FeedbackData> findByUserIdAndStatusOrderByCreateTimeDesc(
            Long userId, FeedbackData.FeedbackStatus status, Pageable pageable);

    Page<FeedbackData> findByUserIdAndFeedbackTypeOrderByCreateTimeDesc(
            Long userId, FeedbackData.FeedbackType type, Pageable pageable);

    Page<FeedbackData> findByUserIdAndFeedbackTypeAndStatusOrderByCreateTimeDesc(
            Long userId, FeedbackData.FeedbackType type, FeedbackData.FeedbackStatus status,
            Pageable pageable);

    /**
     * 查询已接受的单数字反馈（用于生成训练集）
     * 只查询 recognitionResult 为 0-9 的单数字记录
     */
    @Query("SELECT f FROM FeedbackData f " +
            "WHERE f.status = 'ACCEPTED' " +
            "AND f.correctResult BETWEEN 0 AND 9 " +
            "AND (:startTime IS NULL OR f.createTime >= :startTime) " +
            "AND (:endTime IS NULL OR f.createTime <= :endTime) " +
            "AND (:minQualityScore IS NULL OR f.qualityScore >= :minQualityScore) " +
            "ORDER BY f.correctResult ASC")
    List<FeedbackData> findAcceptedSingleDigitFeedback(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("minQualityScore") Integer minQualityScore
    );

    /**
     * 统计已接受的单数字反馈数量（按数字分组）
     */
    @Query("SELECT f.correctResult as digit, COUNT(f) as count FROM FeedbackData f " +
            "WHERE f.status = 'ACCEPTED' " +
            "AND f.correctResult BETWEEN 0 AND 9 " +
            "GROUP BY f.correctResult " +
            "ORDER BY f.correctResult ASC")
    List<Object[]> countAcceptedFeedbackByDigit();

}