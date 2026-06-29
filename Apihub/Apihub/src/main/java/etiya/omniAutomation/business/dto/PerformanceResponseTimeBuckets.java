package etiya.omniAutomation.business.dto;

public record PerformanceResponseTimeBuckets(
        long under500ms,
        long from500msTo1s,
        long from1sTo3s,
        long over3s
) {
}
