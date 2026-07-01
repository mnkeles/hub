package etiya.omniAutomation.business.dto;

public record PerformanceStepInsight(
        String stepName,
        PerformanceInsightSeverity severity,
        PerformanceBottleneckType bottleneckType,
        String metric,
        String actual,
        String expected,
        String explanation
) {
}
