package etiya.omniAutomation.service;

import java.util.List;
import java.util.Map;

public record PerformanceDatasetRuntimeContext(
        Long datasetId,
        Map<String, String> mapping,
        List<Map<String, Object>> rows
) {

    public static PerformanceDatasetRuntimeContext empty() {
        return new PerformanceDatasetRuntimeContext(null, Map.of(), List.of());
    }

    public boolean enabled() {
        return datasetId != null && rows != null && !rows.isEmpty();
    }
}
