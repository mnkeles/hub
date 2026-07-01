package etiya.omniAutomation.business.dto;

public record PerformanceManagementStepAssessment(
        String stepName,
        PerformanceManagementStepStatus status,
        PerformanceManagementRiskLevel priority,
        String mainReason,
        String evidence,
        String impact,
        String recommendation,
        long sampleCount,
        long successCount,
        long failureCount,
        double errorRate,
        double averageMs,
        double p90Ms,
        double p95Ms,
        double p99Ms,
        double throughputPerSecond,
        String lastError
) {
}
