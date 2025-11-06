// RecognitionRecordRepository.java - 识别记录数据访问
package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.RecognitionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * 统计用户识别总数
     */
    @Query("SELECT COUNT(r) FROM RecognitionRecord r WHERE r.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM RecognitionRecord r WHERE r.modelId = :modelId")
    Long countByModelId(@Param("modelId") Long modelId);

    /**
     * 统计用户在指定时间段内的识别次数
     */
    @Query("SELECT COUNT(r) FROM RecognitionRecord r WHERE r.userId = :userId AND r.createTime BETWEEN :startTime AND :endTime")
    Long countByUserIdAndCreateTimeBetween(@Param("userId") Long userId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 统计用户各数字识别次数
     */
    @Query("SELECT r.recognitionResult, COUNT(r) FROM RecognitionRecord r WHERE r.userId = :userId GROUP BY r.recognitionResult")
    List<Object[]> countByUserIdAndRecognitionResult(@Param("userId") Long userId);

    /**
     * 计算用户平均置信度
     */
    @Query("SELECT AVG(r.confidence) FROM RecognitionRecord r WHERE r.userId = :userId")
    BigDecimal avgConfidenceByUserId(@Param("userId") Long userId);

    /**
     * 获取用户最高置信度
     */
    @Query("SELECT MAX(r.confidence) FROM RecognitionRecord r WHERE r.userId = :userId")
    BigDecimal maxConfidenceByUserId(@Param("userId") Long userId);

    /**
     * 获取用户最低置信度
     */
    @Query("SELECT MIN(r.confidence) FROM RecognitionRecord r WHERE r.userId = :userId")
    BigDecimal minConfidenceByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM RecognitionRecord r WHERE r.userId = :userId AND r.isCorrect = true")
    Long countCorrectByUserId(@Param("userId") Long userId);

    @Query("SELECT AVG(r.processingTime) FROM RecognitionRecord r WHERE r.userId = :userId")
    Double avgProcessingTimeByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(r) FROM RecognitionRecord r WHERE r.userId = :userId AND r.createTime >= :startTime")
    Long countByUserIdAndCreateTimeAfter(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    @Query("SELECT r FROM RecognitionRecord r WHERE " +
            "(:result IS NULL OR r.recognitionResult = :result) " +
            "AND (:startTime IS NULL OR r.createTime >= :startTime) " +
            "AND (:endTime IS NULL OR r.createTime <= :endTime) " +
            "ORDER BY r.createTime DESC")
    Page<RecognitionRecord> findAllWithFilters(@Param("result") Integer result,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               Pageable pageable);

    @Query("SELECT r FROM RecognitionRecord r WHERE " +
            "(:userId IS NULL OR r.userId = :userId) " +
            "AND (:result IS NULL OR r.recognitionResult = :result) " +
            "AND (:startTime IS NULL OR r.createTime >= :startTime) " +
            "AND (:endTime IS NULL OR r.createTime <= :endTime) " +
            "ORDER BY r.createTime DESC")
    Page<RecognitionRecord> findAllWithFiltersAndUser(@Param("result") Integer result,
                                                      @Param("userId") Long userId,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime,
                                                      Pageable pageable);

}