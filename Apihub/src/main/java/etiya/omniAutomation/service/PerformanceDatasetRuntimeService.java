package etiya.omniAutomation.service;

import etiya.omniAutomation.entity.PerformanceDatasetEntity;
import etiya.omniAutomation.entity.PerformanceDatasetRowEntity;
import etiya.omniAutomation.repository.PerformanceDatasetRepository;
import etiya.omniAutomation.repository.PerformanceDatasetRowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PerformanceDatasetRuntimeService {

    private final PerformanceDatasetRepository datasetRepository;
    private final PerformanceDatasetRowRepository rowRepository;

    public PerformanceDatasetRuntimeContext resolve(Long projectId, Long testDataId, Map<String, String> requestMapping) {
        if (testDataId == null) {
            return PerformanceDatasetRuntimeContext.empty();
        }
        PerformanceDatasetEntity dataset = datasetRepository.findById(testDataId)
                .orElseThrow(() -> notFound("Dataset not found: " + testDataId));
        if (!Boolean.TRUE.equals(dataset.getActive())) {
            throw badRequest("Dataset is inactive: " + testDataId);
        }
        if (projectId == null || !projectId.equals(dataset.getProjectId())) {
            throw badRequest("Dataset does not belong to the selected project.");
        }
        List<Map<String, Object>> rows = rowRepository.findByDatasetIdAndActiveTrueOrderByRowIndexAsc(testDataId)
                .stream()
                .map(PerformanceDatasetRowEntity::getData)
                .filter(data -> data != null && !data.isEmpty())
                .toList();
        if (rows.isEmpty()) {
            throw badRequest("Dataset must contain at least one active row.");
        }

        Map<String, String> mapping = effectiveMapping(requestMapping, dataset.getDefaultMapping());
        if (mapping.isEmpty()) {
            throw badRequest("Dataset mapping is required when a dataset is selected.");
        }
        validateMapping(mapping, rows);
        return new PerformanceDatasetRuntimeContext(testDataId, mapping, rows);
    }

    public void applyRow(PerformanceDatasetRuntimeContext context, Map<String, String> parameterContext, int threadNumber, int loopIndex) {
        if (context == null || !context.enabled()) {
            return;
        }
        if (parameterContext == null) {
            throw badRequest("Process flow parameter context is required for dataset mapping.");
        }
        int rowIndex = Math.floorMod(threadNumber + loopIndex, context.rows().size());
        Map<String, Object> row = context.rows().get(rowIndex);
        context.mapping().forEach((parameterName, datasetField) -> {
            Object value = row.get(datasetField);
            parameterContext.put(parameterName, value == null ? "" : String.valueOf(value));
        });
    }

    private Map<String, String> effectiveMapping(Map<String, String> requestMapping, Map<String, String> defaultMapping) {
        Map<String, String> source = requestMapping != null && !requestMapping.isEmpty() ? requestMapping : defaultMapping;
        Map<String, String> effective = new LinkedHashMap<>();
        if (source == null) {
            return effective;
        }
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                effective.put(key, value);
            }
        });
        return effective;
    }

    private void validateMapping(Map<String, String> mapping, List<Map<String, Object>> rows) {
        for (String datasetField : mapping.values()) {
            boolean exists = rows.stream().anyMatch(row -> row.containsKey(datasetField));
            if (!exists) {
                throw badRequest("Dataset mapping field not found: " + datasetField);
            }
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
