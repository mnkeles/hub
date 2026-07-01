package etiya.omniAutomation.business.dto;

public record PerformanceAiActionItem(
        String priority,
        String title,
        String description,
        String relatedStepName,
        String relatedMetric
) {
}
