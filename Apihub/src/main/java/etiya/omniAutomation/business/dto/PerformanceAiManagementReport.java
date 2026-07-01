package etiya.omniAutomation.business.dto;

import java.util.Date;
import java.util.List;

public record PerformanceAiManagementReport(
        boolean generated,
        Date generatedAt,
        String model,
        String executiveNarrative,
        String technicalNarrative,
        String rootCauseNarrative,
        List<PerformanceAiActionItem> recommendedActionPlan,
        String releaseReadinessNarrative,
        List<String> limitations,
        String errorMessage,
        Integer schemaVersion,
        String generatedByVersion,
        Long durationMs,
        Integer attemptCount,
        String failureReason,
        List<String> validationErrors,
        String promptHash,
        String inputSummaryHash,
        Integer responseSize,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
    public static PerformanceAiManagementReport notGenerated(String errorMessage) {
        return notGenerated(errorMessage, errorMessage, List.of(), null, null, null, null);
    }

    public static PerformanceAiManagementReport notGenerated(
            String errorMessage,
            String failureReason,
            List<String> validationErrors,
            Long durationMs,
            String promptHash,
            String inputSummaryHash,
            Integer responseSize
    ) {
        return new PerformanceAiManagementReport(
                false,
                new Date(),
                null,
                null,
                null,
                null,
                List.of(),
                null,
                List.of(),
                errorMessage,
                etiya.omniAutomation.service.PerformanceReportVersions.AI_REPORT_SCHEMA_VERSION,
                etiya.omniAutomation.service.PerformanceReportVersions.AI_REPORT_GENERATOR_VERSION,
                durationMs,
                1,
                failureReason,
                validationErrors == null ? List.of() : validationErrors,
                promptHash,
                inputSummaryHash,
                responseSize,
                null,
                null,
                null
        );
    }
}
