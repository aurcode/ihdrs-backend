package com.ihdrs.backend.service;

import com.ihdrs.backend.dto.response.PerformanceMetricsDTO;
import com.ihdrs.backend.dto.response.RecognitionHistoryDTO;
import com.ihdrs.backend.dto.response.StatsDTO;
import com.ihdrs.backend.repository.StatsRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsRepository statsRepository;
    private final EntityManager entityManager;

    public StatsDTO getDashboardStats() {
        // 获取最近24小时的数据
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(24);
        
        return statsRepository.getRecognitionStats(startTime, endTime);
    }

    public StatsDTO getRecognitionStats(LocalDateTime startTime, LocalDateTime endTime) {
        return statsRepository.getRecognitionStats(startTime, endTime);
    }

    public List<RecognitionHistoryDTO> getRecentRecognitions(int limit) {
        return statsRepository.getRecentRecognitions(limit);
    }

    public PerformanceMetricsDTO getPerformanceMetrics() {
        PerformanceMetricsDTO metrics = new PerformanceMetricsDTO();
        
        // 获取系统性能指标
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();
        
        // CPU使用率（近似值）
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            metrics.setCpuUsage(sunOsBean.getSystemCpuLoad() * 100);
        }
        
        // 内存使用率
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        metrics.setMemoryUsage((double) usedMemory / totalMemory * 100);
        
        // 获取最近1小时的请求统计
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusHours(1);
        List<Object[]> hourlyStats = statsRepository.getHourlyStats(startTime, endTime);
        
        Map<String, Long> requestsByHour = new HashMap<>();
        long totalRequests = 0;
        
        for (Object[] stat : hourlyStats) {
            String hour = (String) stat[0];
            Long count = ((Number) stat[1]).longValue();
            requestsByHour.put(hour, count);
            totalRequests += count;
        }
        
        metrics.setTotalRequests(totalRequests);
        metrics.setRequestsByHour(requestsByHour);
        metrics.setActiveUsers(estimateActiveUsers()); // 估算活跃用户
        
        // 系统信息
        PerformanceMetricsDTO.SystemInfoDTO systemInfo = new PerformanceMetricsDTO.SystemInfoDTO();
        systemInfo.setOsName(osBean.getName() + " " + osBean.getVersion());
        systemInfo.setJavaVersion(System.getProperty("java.version"));
        systemInfo.setApplicationVersion("1.0.0"); // 从配置文件中读取
        systemInfo.setUptime(ManagementFactory.getRuntimeMXBean().getUptime());
        
        metrics.setSystemInfo(systemInfo);
        
        return metrics;
    }

    private Long estimateActiveUsers() {
        String sql = """
            SELECT COUNT(DISTINCT user_id)
            FROM recognition_records
            WHERE user_id IS NOT NULL
            AND create_time >= :recentTime
        """;
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(5);
        Object result = entityManager.createNativeQuery(sql)
                .setParameter("recentTime", recentTime)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    public Map<String, Object> getErrorAnalysis() {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(7);

        String totalSql = """
            SELECT COUNT(*)
            FROM recognition_records
            WHERE is_correct = 0
            AND create_time BETWEEN :start AND :end
        """;
        Long totalErrors = ((Number) entityManager.createNativeQuery(totalSql)
                .setParameter("start", startTime)
                .setParameter("end", endTime)
                .getSingleResult()).longValue();

        String groupSql = """
            SELECT recognition_result, COUNT(*)
            FROM recognition_records
            WHERE is_correct = 0
            AND create_time BETWEEN :start AND :end
            GROUP BY recognition_result
            ORDER BY COUNT(*) DESC
        """;
        List<Object[]> results = entityManager.createNativeQuery(groupSql)
                .setParameter("start", startTime)
                .setParameter("end", endTime)
                .getResultList();

        List<Map<String, Object>> commonErrors = results.stream()
                .map(r -> Map.<String, Object>of(
                        "error", "识别错误结果: " + r[0],
                        "count", ((Number) r[1]).longValue()
                ))
                .toList();

        return Map.of(
                "period", "7 days",
                "totalErrors", totalErrors,
                "commonErrors", commonErrors
        );
    }
}