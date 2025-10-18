// TrainingTaskRepository.java - 训练任务数据访问
package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.TrainingTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrainingTaskRepository extends JpaRepository<TrainingTask, Long> {

    /**
     * 根据创建者分页查询训练任务
     */
    Page<TrainingTask> findByCreatorIdOrderByCreateTimeDesc(Long creatorId, Pageable pageable);

    /**
     * 根据状态分页查询训练任务
     */
    Page<TrainingTask> findByStatusOrderByCreateTimeDesc(TrainingTask.TaskStatus status, Pageable pageable);

    /**
     * 查询正在运行的训练任务
     */
    List<TrainingTask> findByStatusIn(List<TrainingTask.TaskStatus> statuses);

    /**
     * 根据任务名称查找任务
     */
    List<TrainingTask> findByTaskNameContaining(String taskName);

    /**
     * 查询指定时间段内完成的任务数量
     */
    @Query("SELECT COUNT(t) FROM TrainingTask t WHERE t.status = 'COMPLETED' AND t.endTime BETWEEN :startTime AND :endTime")
    Long countCompletedTasksByEndTimeBetween(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定时间段内失败的任务数量
     */
    @Query("SELECT COUNT(t) FROM TrainingTask t WHERE t.status = 'FAILED' AND t.endTime BETWEEN :startTime AND :endTime")
    Long countFailedTasksByEndTimeBetween(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各状态任务数量
     */
    @Query("SELECT t.status, COUNT(t) FROM TrainingTask t GROUP BY t.status")
    List<Object[]> countByStatus();

    /**
     * 查询最近的训练任务
     */
    @Query("SELECT t FROM TrainingTask t ORDER BY t.createTime DESC")
    List<TrainingTask> findRecentTasks(Pageable pageable);

    /**
     * 查询用户最佳准确率的任务
     */
    @Query("SELECT t FROM TrainingTask t WHERE t.creatorId = :creatorId AND t.finalAccuracy IS NOT NULL ORDER BY t.finalAccuracy DESC")
    List<TrainingTask> findBestTasksByCreator(@Param("creatorId") Long creatorId, Pageable pageable);
}