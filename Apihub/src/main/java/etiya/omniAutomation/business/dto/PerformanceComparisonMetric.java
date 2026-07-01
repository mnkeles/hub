package etiya.omniAutomation.business.dto;

public record PerformanceComparisonMetric(
        String metricName,
        Object baseValue,
        Object targetValue,
        Object delta,
        String direction,
        Boolean improvement
) {
}
