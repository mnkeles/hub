package etiya.omniAutomation.business.dto;

import java.util.List;

public record PerformanceComparisonResult(
        Long baseResultId,
        Long targetResultId,
        List<PerformanceComparisonMetric> metrics
) {
}
