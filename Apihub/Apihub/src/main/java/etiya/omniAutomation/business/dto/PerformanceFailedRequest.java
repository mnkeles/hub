package etiya.omniAutomation.business.dto;

public record PerformanceFailedRequest(
        Integer threadNumber,
        String stepName,
        Double elapsedTime,
        String errorType,
        String errorMessage
) {
}
