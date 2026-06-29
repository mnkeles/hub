package etiya.omniAutomation.business.dto;

public record PerformanceManagementSlaItem(
        String metric,
        boolean passed,
        String expected,
        String actual,
        String explanation
) {
}
