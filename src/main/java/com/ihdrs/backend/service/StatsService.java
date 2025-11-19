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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsRepository statsRepository;
    private final EntityManager entityManager;

    public StatsDTO getDashboardStats() {
        StatsDTO stats = statsRepository.getRecognitionStats(null, null);

        // 加入用户数量
        stats.setTotalUsers(statsRepository.getUserCount());

        // 加入模型数量
        stats.setTotalModels(statsRepository.getModelCount());

        // 今日识别次数
        stats.setTodayRecognitions(statsRepository.getTodayRecognitions());

        // 你可以先将增长率填为 0（或之后再实现）
        stats.setRecognitionGrowth(0.0);
        stats.setUserGrowth(0.0);
        stats.setModelGrowth(0.0);
        stats.setTodayGrowth(0.0);

        return stats;
    }


    public StatsDTO getRecognitionStats(LocalDateTime startTime, LocalDateTime endTime) {
        return statsRepository.getRecognitionStats(startTime, endTime);
    }

    public List<RecognitionHistoryDTO> getRecentRecognitions(int limit) {
        return statsRepository.getRecentRecognitions(limit);
    }

    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // 获取系统性能指标
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();

        // CPU使用率
        double cpuUsage = 0.0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean =
                    (com.sun.management.OperatingSystemMXBean) osBean;
            cpuUsage = sunOsBean.getSystemCpuLoad() * 100;
        }

        // 内存使用率
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsage = (double) usedMemory / totalMemory * 100;

        // 获取最近7天的识别量统计
        List<Map<String, Object>> weeklyTrend = getWeeklyRecognitionTrend();

        // 获取最近7天的成功率趋势
        List<Map<String, Object>> successRateTrend = getSuccessRateTrend();

        // 获取数字分布统计
        List<Map<String, Object>> digitDistribution = getDigitDistribution();

        // 获取小时级别的识别量（今天）
        List<Map<String, Object>> hourlyRecognitions = getHourlyRecognitions();

        // 活跃用户数
        long activeUsers = estimateActiveUsers();
        long totalRequests = getTotalRequests();

        // 系统资源使用历史（最近24小时）
        List<Map<String, Object>> resourceUsageHistory = getResourceUsageHistory();

        metrics.put("cpuUsage", Math.round(cpuUsage * 100.0) / 100.0);
        metrics.put("memoryUsage", Math.round(memoryUsage * 100.0) / 100.0);
        metrics.put("totalRequests", totalRequests);
        metrics.put("activeUsers", activeUsers);
        metrics.put("weeklyTrend", weeklyTrend);
        metrics.put("successRateTrend", successRateTrend);
        metrics.put("digitDistribution", digitDistribution);
        metrics.put("hourlyRecognitions", hourlyRecognitions);
        metrics.put("resourceUsageHistory", resourceUsageHistory);

        // 系统信息
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("osName", osBean.getName() + " " + osBean.getVersion());
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("applicationVersion", "1.0.0");
        systemInfo.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime());
        metrics.put("systemInfo", systemInfo);

        return metrics;
    }

    /**
     * 获取最近7天的识别量趋势
     */
    private List<Map<String, Object>> getWeeklyRecognitionTrend() {
        String sql = """
            SELECT DATE(create_time) as date, COUNT(*) as count
            FROM recognition_records
            WHERE create_time >= :startTime
            GROUP BY DATE(create_time)
            ORDER BY DATE(create_time)
        """;

        LocalDateTime startTime = LocalDateTime.now().minusDays(6).toLocalDate().atStartOfDay();
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("startTime", startTime)
                .getResultList();

        // 填充缺失的日期
        Map<LocalDate, Long> dataMap = new HashMap<>();
        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long count = ((Number) row[1]).longValue();
            dataMap.put(date, count);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.format(formatter));
            point.put("count", dataMap.getOrDefault(date, 0L));
            trend.add(point);
        }

        return trend;
    }

    /**
     * 获取最近7天的成功率趋势
     */
    private List<Map<String, Object>> getSuccessRateTrend() {
        String sql = """
            SELECT 
                DATE(create_time) as date,
                COUNT(*) as total,
                SUM(CASE WHEN is_correct = 1 THEN 1 ELSE 0 END) as success
            FROM recognition_records
            WHERE create_time >= :startTime
            GROUP BY DATE(create_time)
            ORDER BY DATE(create_time)
        """;

        LocalDateTime startTime = LocalDateTime.now().minusDays(6).toLocalDate().atStartOfDay();
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("startTime", startTime)
                .getResultList();

        Map<LocalDate, Double> rateMap = new HashMap<>();
        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            Long total = ((Number) row[1]).longValue();
            Long success = ((Number) row[2]).longValue();
            double rate = total > 0 ? (success * 100.0 / total) : 0.0;
            rateMap.put(date, rate);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.format(formatter));
            point.put("rate", Math.round(rateMap.getOrDefault(date, 0.0) * 100.0) / 100.0);
            trend.add(point);
        }

        return trend;
    }

    /**
     * 获取数字识别分布
     */
    private List<Map<String, Object>> getDigitDistribution() {
        String sql = """
            SELECT recognition_result, COUNT(*) as count
            FROM recognition_records
            WHERE create_time >= :startTime
            GROUP BY recognition_result
            ORDER BY recognition_result
        """;

        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        List<Object[]> results = entityManager.createNativeQuery(sql)
                .setParameter("startTime", startTime)
                .getResultList();

        return results.stream()
                .map(row -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("digit", row[0]);
                    item.put("count", ((Number) row[1]).longValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取今天每小时的识别量
     */
    private List<Map<String, Object>> getHourlyRecognitions() {
        String sql = """
            SELECT HOUR(create_time) as hour, COUNT(*) as count
            FROM recognition_records
            WHERE DATE(create_time) = CURDATE()
            GROUP BY HOUR(create_time)
            ORDER BY HOUR(create_time)
        """;

        List<Object[]> results = entityManager.createNativeQuery(sql)
                .getResultList();

        Map<Integer, Long> hourMap = new HashMap<>();
        for (Object[] row : results) {
            Integer hour = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            hourMap.put(hour, count);
        }

        List<Map<String, Object>> hourlyData = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("hour", String.format("%02d:00", i));
            point.put("count", hourMap.getOrDefault(i, 0L));
            hourlyData.add(point);
        }

        return hourlyData;
    }

    /**
     * 获取系统资源使用历史（实际应该存储到数据库）
     */
    private List<Map<String, Object>> getResourceUsageHistory() {
        List<Map<String, Object>> history = new ArrayList<>();

        // 这里返回最近12个小时的模拟数据
        // 实际生产环境应该从监控系统或数据库中读取
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Runtime runtime = Runtime.getRuntime();

        double currentCpu = 0.0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean =
                    (com.sun.management.OperatingSystemMXBean) osBean;
            currentCpu = sunOsBean.getSystemCpuLoad() * 100;
        }

        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        double currentMemory = (double) (totalMemory - freeMemory) / totalMemory * 100;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        for (int i = 11; i >= 0; i--) {
            Map<String, Object> point = new HashMap<>();
            LocalDateTime time = LocalDateTime.now().minusHours(i);
            point.put("time", time.format(formatter));
            // 添加一些随机波动使图表更真实
            point.put("cpu", Math.round((currentCpu + (Math.random() - 0.5) * 10) * 100.0) / 100.0);
            point.put("memory", Math.round((currentMemory + (Math.random() - 0.5) * 5) * 100.0) / 100.0);
            history.add(point);
        }

        return history;
    }

    private Long estimateActiveUsers() {
        String sql = """
            SELECT COUNT(DISTINCT user_id)
            FROM recognition_records
            WHERE user_id IS NOT NULL
            AND create_time >= :recentTime
        """;
        LocalDateTime recentTime = LocalDateTime.now().minusMinutes(30);
        Object result = entityManager.createNativeQuery(sql)
                .setParameter("recentTime", recentTime)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    private Long getTotalRequests() {
        String sql = """
            SELECT COUNT(*)
            FROM recognition_records
            WHERE create_time >= :startTime
        """;
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        Object result = entityManager.createNativeQuery(sql)
                .setParameter("startTime", startTime)
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