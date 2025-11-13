package com.ihdrs.backend.repository;

import com.ihdrs.backend.dto.response.RecognitionHistoryDTO;
import com.ihdrs.backend.dto.response.StatsDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class StatsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public StatsDTO getRecognitionStats(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
            SELECT 
                COUNT(*) as total_recognitions,
                SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as success_recognitions,
                SUM(CASE WHEN is_correct = 0 THEN 1 ELSE 0 END) as failed_recognitions,
                AVG(processing_time) as avg_processing_time
            FROM recognition_records 
            WHERE create_time BETWEEN ?1 AND ?2
            """;

        Object[] result = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter(1, startTime)
                .setParameter(2, endTime)
                .getSingleResult();

        StatsDTO stats = new StatsDTO();
        stats.setTotalRecognitions(((Number) result[0]).longValue());
        stats.setSuccessRecognitions(((Number) result[1]).longValue());
        stats.setFailedRecognitions(((Number) result[2]).longValue());
        
        Long total = stats.getTotalRecognitions();
        if (total > 0) {
            stats.setSuccessRate(stats.getSuccessRecognitions() * 100.0 / total);
            stats.setErrorRate(stats.getFailedRecognitions() * 100.0 / total);
        } else {
            stats.setSuccessRate(0.0);
            stats.setErrorRate(0.0);
        }
        
        stats.setAvgProcessingTime(result[3] != null ? ((Number) result[3]).doubleValue() : 0.0);
        stats.setStatsTime(LocalDateTime.now());
        
        return stats;
    }

    public List<RecognitionHistoryDTO> getRecentRecognitions(int limit) {
        String sql = """
            SELECT 
                r.record_id, r.image_path, r.recognition_result, 
                CASE WHEN r.is_correct = 1 THEN 'SUCCESS' ELSE 'FAILED' END as status,
                r.confidence, r.processing_time, r.create_time, 
                m.model_name, u.username
            FROM recognition_records r
            LEFT JOIN models m ON r.model_id = m.model_id
            LEFT JOIN users u ON r.user_id = u.user_id
            ORDER BY r.create_time DESC 
            LIMIT ?1
            """;

        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter(1, limit)
                .getResultList();

        return results.stream().map(this::mapToRecognitionHistoryDTO).toList();
    }

    private RecognitionHistoryDTO mapToRecognitionHistoryDTO(Object[] result) {
        RecognitionHistoryDTO dto = new RecognitionHistoryDTO();
        dto.setId(((Number) result[0]).longValue());
        dto.setImageName((String) result[1]);
        dto.setResult("识别结果: " + result[2]);
        dto.setStatus((String) result[3]);
        dto.setConfidence(result[4] != null ? ((Number) result[4]).doubleValue() : null);
        dto.setProcessingTime(result[5] != null ? ((Number) result[5]).longValue() : null);
        
        if (result[6] != null) {
            dto.setCreateTime(((java.sql.Timestamp) result[6]).toLocalDateTime());
        }
        
        String modelName = (String) result[7];
        String username = (String) result[8];
        dto.setErrorMessage("模型: " + modelName + ", 用户: " + username);
        
        return dto;
    }

    public List<Object[]> getHourlyStats(LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
        SELECT 
            DATE_FORMAT(create_time, '%Y-%m-%d %H:00:00') AS hour,
            COUNT(*) AS request_count
        FROM recognition_records
        WHERE create_time BETWEEN ?1 AND ?2
        GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d %H:00:00')
        ORDER BY hour ASC
        """;

        return entityManager.createNativeQuery(sql)
                .setParameter(1, startTime)
                .setParameter(2, endTime)
                .getResultList();
    }

}