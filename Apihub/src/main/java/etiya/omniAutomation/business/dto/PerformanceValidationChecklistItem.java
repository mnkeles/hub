package etiya.omniAutomation.business.dto;

public record PerformanceValidationChecklistItem(
        String key,
        String label,
        PerformanceValidationStatus status,
        String message
) {
}
