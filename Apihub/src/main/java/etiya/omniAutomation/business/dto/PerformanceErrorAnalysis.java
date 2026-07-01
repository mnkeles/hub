package etiya.omniAutomation.business.dto;

import java.util.List;

public record PerformanceErrorAnalysis(
        long totalErrorCount,
        double errorRate,
        List<PerformanceErrorTypeCount> errorsByType,
        List<PerformanceStepErrorCount> errorsByStep,
        String lastError,
        List<PerformanceFailedRequest> failedRequests
) {
}
