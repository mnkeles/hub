package etiya.omniAutomation.business.dto;

import etiya.omniAutomation.common.GeneralEnums;

import java.util.List;

public record PerformanceLiveSnapshot(
        Long performanceResultId,
        GeneralEnums.PerformanceStatus status,
        int activeThreadCount,
        int totalThreadCount,
        long completedSamples,
        long successfulSamples,
        long failedSamples,
        double errorRate,
        double throughputPerSecond,
        Double averageElapsedTime,
        Double p90ElapsedTime,
        Double p95ElapsedTime,
        long elapsedTimeMs,
        Long estimatedRemainingMs,
        String lastCompletedStep,
        String lastError,
        List<String> warnings,
        boolean metricsAvailable,
        String message
) {
}
