package etiya.omniAutomation.service;

import etiya.omniAutomation.entity.PerformanceDatasetEntity;
import etiya.omniAutomation.entity.PerformanceDatasetRowEntity;
import etiya.omniAutomation.repository.PerformanceDatasetRepository;
import etiya.omniAutomation.repository.PerformanceDatasetRowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerformanceDatasetRuntimeServiceTest {

    private final PerformanceDatasetRepository datasetRepository = mock(PerformanceDatasetRepository.class);
    private final PerformanceDatasetRowRepository rowRepository = mock(PerformanceDatasetRowRepository.class);
    private final PerformanceDatasetRuntimeService service = new PerformanceDatasetRuntimeService(datasetRepository, rowRepository);

    @Test
    void roundRobinUsesThreadPlusLoop() {
        PerformanceDatasetRuntimeContext context = new PerformanceDatasetRuntimeContext(10L, Map.of("customerId", "id"), List.of(
                rowData("id", "row-0"),
                rowData("id", "row-1"),
                rowData("id", "row-2")
        ));
        Map<String, String> parameters = new LinkedHashMap<>();

        service.applyRow(context, parameters, 2, 2);

        assertEquals("row-1", parameters.get("customerId"));
    }

    @Test
    void requestMappingOverridesDefaultMapping() {
        PerformanceDatasetEntity dataset = dataset(Map.of("customerId", "default_id"));
        when(datasetRepository.findById(10L)).thenReturn(Optional.of(dataset));
        when(rowRepository.findByDatasetIdAndActiveTrueOrderByRowIndexAsc(10L)).thenReturn(List.of(row(rowData("id", "42", "default_id", "24"))));

        PerformanceDatasetRuntimeContext context = service.resolve(1L, 10L, Map.of("customerId", "id"));
        Map<String, String> parameters = new LinkedHashMap<>();
        service.applyRow(context, parameters, 0, 0);

        assertEquals("42", parameters.get("customerId"));
    }

    @Test
    void nullValueBecomesEmptyString() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", null);
        PerformanceDatasetRuntimeContext context = new PerformanceDatasetRuntimeContext(10L, Map.of("customerId", "id"), List.of(data));
        Map<String, String> parameters = new LinkedHashMap<>();

        service.applyRow(context, parameters, 0, 0);

        assertEquals("", parameters.get("customerId"));
    }

    @Test
    void missingMappedFieldFails() {
        PerformanceDatasetEntity dataset = dataset(Map.of("customerId", "missing"));
        when(datasetRepository.findById(10L)).thenReturn(Optional.of(dataset));
        when(rowRepository.findByDatasetIdAndActiveTrueOrderByRowIndexAsc(10L)).thenReturn(List.of(row(rowData("id", "42"))));

        assertThrows(ResponseStatusException.class, () -> service.resolve(1L, 10L, Map.of()));
    }

    @Test
    void inactiveDatasetFails() {
        PerformanceDatasetEntity dataset = dataset(Map.of("customerId", "id"));
        dataset.setActive(false);
        when(datasetRepository.findById(10L)).thenReturn(Optional.of(dataset));

        assertThrows(ResponseStatusException.class, () -> service.resolve(1L, 10L, Map.of("customerId", "id")));
    }

    private PerformanceDatasetEntity dataset(Map<String, String> defaultMapping) {
        PerformanceDatasetEntity dataset = new PerformanceDatasetEntity();
        dataset.setDatasetId(10L);
        dataset.setProjectId(1L);
        dataset.setActive(true);
        dataset.setDefaultMapping(defaultMapping);
        return dataset;
    }

    private PerformanceDatasetRowEntity row(Map<String, Object> data) {
        PerformanceDatasetRowEntity row = new PerformanceDatasetRowEntity();
        row.setDatasetId(10L);
        row.setActive(true);
        row.setData(data);
        return row;
    }

    private Map<String, Object> rowData(String key, Object value) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put(key, value);
        return data;
    }

    private Map<String, Object> rowData(String firstKey, Object firstValue, String secondKey, Object secondValue) {
        Map<String, Object> data = rowData(firstKey, firstValue);
        data.put(secondKey, secondValue);
        return data;
    }
}
