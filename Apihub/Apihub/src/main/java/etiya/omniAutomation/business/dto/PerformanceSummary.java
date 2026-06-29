package etiya.omniAutomation.business.dto;

public record PerformanceSummary(
        String stepName,
        double maxElapsedTime,
        double minElapsedTime,
        double averageElapsedTime,
        long sampleCount,
        long successCount,
        long failureCount,
        double errorRate,
        double throughputPerSecond,
        double medianElapsedTime,
        double p90ElapsedTime,
        double p95ElapsedTime,
        double p99ElapsedTime,
        double standardDeviation,
        String lastError,
        PerformanceResponseTimeBuckets responseTimeBuckets
) {

    public PerformanceSummary(String stepName, double maxElapsedTime, double minElapsedTime, double averageElapsedTime) {
        this(stepName, maxElapsedTime, minElapsedTime, averageElapsedTime, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                null, null);
    }
}
