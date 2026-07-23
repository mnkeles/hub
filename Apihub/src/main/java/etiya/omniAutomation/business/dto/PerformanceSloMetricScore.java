package etiya.omniAutomation.business.dto;

public record PerformanceSloMetricScore(
        String metricName,
        double score,
        double maxScore,
        Double actualValue,
        Double targetValue,
        String direction,
        String message
) {
}
