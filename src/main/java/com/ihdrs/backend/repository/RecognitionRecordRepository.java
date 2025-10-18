// RecognitionRecordRepository.java - 识别记录数据访问
package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.RecognitionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecognitionRecordRepository extends JpaRepository<RecognitionRecord, Long> {

    /**
     * 根据用户ID分页查询识别记录
     */
    Page<RecognitionRecord> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据用户ID和时间范围查询识别记录
     */
    Page<RecognitionRecord> findByUserIdAndCreateTimeBetweenOrderByCreateTimeDesc(
            Long userId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据会话ID查询识别记录
     */
    List<RecognitionRecord> findBySessionIdOrderByCreateTimeDesc(String sessionId);

    /**
     * 查询指定时间段内的识别记录数量
     */
    @Query("SELECT COUNT(r) FROM RecognitionRecord r WHERE r.createTime BETWEEN :startTime AND :endTime")
    Long countByCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间段内的平均置信度
     */
    @Query("SELECT AVG(r.confidence) FROM RecognitionRecord r WHERE r.createTime BETWEEN :startTime AND :endTime")
    BigDecimal avgConfidenceByCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间段内的平均处理时间
     */
    @Query("SELECT AVG(r.processingTime) FROM RecognitionRecord r WHERE r.createTime BETWEEN :startTime AND :endTime AND r.processingTime IS NOT NULL")
    Double avgProcessingTimeByCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各数字的识别次数
     */
    @Query("SELECT r.recognitionResult, COUNT(r) FROM RecognitionRecord r WHERE r.createTime BETWEEN :startTime AND :endTime GROUP BY r.recognitionResult")
    List<Object[]> countByRecognitionResultAndCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各输入类型的使用次数
     */
    @Query("SELECT r.inputType, COUNT(r) FROM RecognitionRecord r WHERE r.createTime BETWEEN :startTime AND :endTime GROUP BY r.inputType")
    List<Object[]> countByInputTypeAndCreateTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定模型的识别记录统计
     */
    @Query("SELECT COUNT(r), AVG(r.confidence), AVG(r.processingTime) FROM RecognitionRecord r WHERE r.modelId = :modelId AND r.createTime BETWEEN :startTime AND :endTime")
    List<Object[]> getModelStatistics(@Param("modelId") Long modelId,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 查询准确率（需要有is_correct字段）
     */
    @Query("SELECT COUNT(r), SUM(CASE WHEN r.isCorrect = true THEN 1 ELSE 0 END) FROM RecognitionRecord r WHERE r.createTime BETWEEN :startTime AND :endTime AND r.isCorrect IS NOT NULL")
    List<Object[]> getAccuracyStatistics(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 根据图片哈希查找相似记录
     */
    Optional<RecognitionRecord> findByImageHash(String imageHash);

    /**
     * 查询置信度低于阈值的记录
     */
    Page<RecognitionRecord> findByConfidenceLessThanOrderByCreateTimeDesc(BigDecimal threshold, Pageable pageable);
}