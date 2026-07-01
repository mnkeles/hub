package etiya.omniAutomation.business.dto;

public record PerformanceManagementProblemArea(
        String title,
        String stepName,
        String metric,
        String value,
        String impact
) {
}
