// FeedbackDataRepository.java - 反馈数据访问
package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.FeedbackData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /**
     * 查询指定时间段内的反馈数量
     */
    @Query("SELECT COUNT(f) FROM FeedbackData f WHERE f.createTime BETWEEN :startTime AND :endTime")
    Long countByCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各反馈类型数量
     */
    @Query("SELECT f.feedbackType, COUNT(f) FROM FeedbackData f WHERE f.createTime BETWEEN :startTime AND :endTime GROUP BY f.feedbackType")
    List<Object[]> countByFeedbackTypeAndCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 查询已接受的反馈数据（用于重新训练）
     */
    @Query("SELECT f FROM FeedbackData f WHERE f.status = 'ACCEPTED' ORDER BY f.createTime DESC")
    List<FeedbackData> findAcceptedFeedback();

    /**
     * 统计用户反馈的准确率改进
     */
    @Query("SELECT f.originalResult, f.correctResult, COUNT(f) FROM FeedbackData f WHERE f.status = 'ACCEPTED' GROUP BY f.originalResult, f.correctResult")
    List<Object[]> getFeedbackAccuracyImprovement();
}