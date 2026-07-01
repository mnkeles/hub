package etiya.omniAutomation.business.dto;

import java.util.List;

public record PerformanceThresholdResult(
        boolean passed,
        String statusLabel,
        List<String> reasons,
        PerformanceThresholdConfig thresholds
) {
}
