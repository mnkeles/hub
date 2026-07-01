package etiya.omniAutomation.business.dto;

public record PerformanceMetricInsight(
        String metric,
        PerformanceInsightSeverity severity,
        String actual,
        String expected,
        String explanation
) {
}
