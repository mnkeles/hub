package etiya.omniAutomation.business.dto;

import java.util.Date;
import java.util.Map;

public record PerformanceDatasetDto(
        Long datasetId,
        Long projectId,
        String name,
        String description,
        PerformanceDatasetSourceType sourceType,
        Map<String, Object> columnSchema,
        Map<String, String> defaultMapping,
        Integer rowCount,
        Boolean active,
        Date createdAt,
        Date updatedAt
) {
}
