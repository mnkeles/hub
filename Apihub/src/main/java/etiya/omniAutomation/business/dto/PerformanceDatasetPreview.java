package etiya.omniAutomation.business.dto;

import java.util.List;

public record PerformanceDatasetPreview(
        PerformanceDatasetDto dataset,
        List<PerformanceDatasetRowDto> rows
) {
}
