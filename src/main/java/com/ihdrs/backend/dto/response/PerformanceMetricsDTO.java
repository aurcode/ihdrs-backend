package com.ihdrs.backend.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class PerformanceMetricsDTO {
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Long totalRequests;
    private Long activeUsers;
    private Map<String, Long> requestsByHour;
    private SystemInfoDTO systemInfo;
    @Data
    public static class SystemInfoDTO {
        private String osName;
        private String javaVersion;
        private String applicationVersion;
        private Long uptime;
    }
}