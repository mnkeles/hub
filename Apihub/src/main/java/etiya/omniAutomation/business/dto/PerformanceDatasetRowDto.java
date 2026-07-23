package etiya.omniAutomation.business.dto;

import java.util.Map;

public record PerformanceDatasetRowDto(
        Long rowId,
        Long datasetId,
        Integer rowIndex,
        Map<String, Object> data,
        Boolean active
) {
}
