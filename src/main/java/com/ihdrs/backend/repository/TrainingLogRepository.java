// TrainingLogRepository.java
package com.ihdrs.backend.repository;

import com.ihdrs.backend.entity.TrainingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrainingLogRepository extends JpaRepository<TrainingLog, Long> {

    List<TrainingLog> findByTaskIdOrderByEpochAsc(Long taskId);

    @Query("SELECT l FROM TrainingLog l WHERE l.taskId = :taskId AND l.epoch = :epoch ORDER BY l.step")
    List<TrainingLog> findByTaskIdAndEpoch(@Param("taskId") Long taskId, @Param("epoch") Integer epoch);

    @Query("SELECT MAX(l.epoch) FROM TrainingLog l WHERE l.taskId = :taskId")
    Integer findMaxEpochByTaskId(@Param("taskId") Long taskId);

    void deleteByTaskId(Long taskId);
}
