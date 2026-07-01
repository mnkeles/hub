package etiya.omniAutomation.business.dto;

public record PerformanceThresholdConfig(
        double maxErrorRatePercent,
        double maxAverageMs,
        double maxP95Ms,
        double maxP99Ms,
        double minThroughputPerSecond
) {

    public static PerformanceThresholdConfig defaults() {
        return new PerformanceThresholdConfig(1, 1000, 3000, 5000, 20);
    }
}
