package etiya.omniAutomation.business.dto;

import etiya.omniAutomation.common.GeneralEnums;

import java.util.Date;

public record PerformanceRunSummary(
        GeneralEnums.PerformanceStatus status,
        Date startedAt,
        Date completedAt,
        long totalDurationMs,
        int threadCount,
        int rampUpPeriod,
        long totalSamples,
        long successfulSamples,
        long failedSamples,
        double errorRate,
        double throughputPerSecond,
        double averageElapsedTime,
        double minElapsedTime,
        double maxElapsedTime,
        double p50ElapsedTime,
        double p90ElapsedTime,
        double p95ElapsedTime,
        double p99ElapsedTime,
        String slowestStepName
) {
}
