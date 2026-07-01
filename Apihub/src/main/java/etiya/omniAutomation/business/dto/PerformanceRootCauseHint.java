package etiya.omniAutomation.business.dto;

public record PerformanceRootCauseHint(
        PerformanceInsightSeverity severity,
        String category,
        String signal,
        String explanation,
        String recommendation
) {
}
