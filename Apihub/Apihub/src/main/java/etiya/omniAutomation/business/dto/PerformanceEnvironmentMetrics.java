package etiya.omniAutomation.business.dto;

import java.util.List;

public record PerformanceEnvironmentMetrics(
        boolean metricsAvailable,
        String message,
        Double cpuAvgPercent,
        Double cpuMaxPercent,
        Double memoryAvgPercent,
        Double memoryMaxPercent,
        Double jvmHeapMaxPercent,
        Long gcTimeMs,
        Integer dbActiveConnectionMax,
        Integer dbConnectionPoolSize,
        Integer slowSqlCount,
        Integer http5xxCount,
        Integer podRestartCount,
        List<String> warnings
) {
}
