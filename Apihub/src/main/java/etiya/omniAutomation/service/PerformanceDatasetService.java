package etiya.omniAutomation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.PerformanceDatasetDto;
import etiya.omniAutomation.business.dto.PerformanceDatasetPreview;
import etiya.omniAutomation.business.dto.PerformanceDatasetRowDto;
import etiya.omniAutomation.business.dto.PerformanceDatasetSourceType;
import etiya.omniAutomation.entity.PerformanceDatasetEntity;
import etiya.omniAutomation.entity.PerformanceDatasetRowEntity;
import etiya.omniAutomation.repository.PerformanceDatasetRepository;
import etiya.omniAutomation.repository.PerformanceDatasetRowRepository;
import etiya.omniAutomation.request.PerformanceDatasetRequest;
import etiya.omniAutomation.request.PerformanceDatasetRowRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PerformanceDatasetService {

    private final PerformanceDatasetRepository datasetRepository;
    private final PerformanceDatasetRowRepository rowRepository;
    private final PerformanceDatasetParser parser;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<PerformanceDatasetDto> list(Long projectId) {
        if (projectId == null) {
            throw badRequest("projectId is required.");
        }
        return datasetRepository.findByProjectIdAndActiveTrueOrderByUpdatedAtDesc(projectId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PerformanceDatasetPreview preview(Long datasetId) {
        PerformanceDatasetEntity dataset = activeDataset(datasetId);
        List<PerformanceDatasetRowDto> rows = rowRepository.findTop20ByDatasetIdAndActiveTrueOrderByRowIndexAsc(datasetId)
                .stream()
                .map(this::toRowDto)
                .toList();
        return new PerformanceDatasetPreview(toDto(dataset), rows);
    }

    @Transactional
    public PerformanceDatasetDto create(PerformanceDatasetRequest request) {
        validateDatasetRequest(request);
        Date now = new Date();
        PerformanceDatasetEntity entity = new PerformanceDatasetEntity();
        entity.setProjectId(request.getProjectId());
        entity.setName(request.getName().trim());
        entity.setDescription(blankToNull(request.getDescription()));
        entity.setSourceType(PerformanceDatasetSourceType.MANUAL);
        entity.setDefaultMapping(safeMapping(request.getDefaultMapping()));
        entity.setColumnSchema(schemaFromMapping(request.getDefaultMapping()));
        entity.setRowCount(0);
        entity.setActive(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toDto(datasetRepository.save(entity));
    }

    @Transactional
    public PerformanceDatasetDto update(Long datasetId, PerformanceDatasetRequest request) {
        validateDatasetRequest(request);
        PerformanceDatasetEntity entity = activeDataset(datasetId);
        if (!entity.getProjectId().equals(request.getProjectId())) {
            throw badRequest("Dataset project cannot be changed.");
        }
        entity.setName(request.getName().trim());
        entity.setDescription(blankToNull(request.getDescription()));
        entity.setDefaultMapping(safeMapping(request.getDefaultMapping()));
        entity.setUpdatedAt(new Date());
        return toDto(datasetRepository.save(entity));
    }

    @Transactional
    public void deactivate(Long datasetId) {
        PerformanceDatasetEntity entity = activeDataset(datasetId);
        entity.setActive(false);
        entity.setUpdatedAt(new Date());
        datasetRepository.save(entity);
    }

    @Transactional
    public PerformanceDatasetRowDto addRow(Long datasetId, PerformanceDatasetRowRequest request) {
        PerformanceDatasetEntity dataset = activeDataset(datasetId);
        Map<String, Object> data = validateRowRequest(request);
        PerformanceDatasetRowEntity row = new PerformanceDatasetRowEntity();
        row.setDatasetId(datasetId);
        row.setRowIndex((int) rowRepository.countByDatasetIdAndActiveTrue(datasetId));
        row.setData(data);
        row.setActive(true);
        PerformanceDatasetRowEntity saved = rowRepository.save(row);
        refreshDatasetStats(dataset);
        return toRowDto(saved);
    }

    @Transactional
    public PerformanceDatasetRowDto updateRow(Long datasetId, Long rowId, PerformanceDatasetRowRequest request) {
        PerformanceDatasetEntity dataset = activeDataset(datasetId);
        Map<String, Object> data = validateRowRequest(request);
        PerformanceDatasetRowEntity row = rowRepository.findById(rowId)
                .orElseThrow(() -> notFound("Dataset row not found: " + rowId));
        if (!datasetId.equals(row.getDatasetId()) || !Boolean.TRUE.equals(row.getActive())) {
            throw notFound("Dataset row not found: " + rowId);
        }
        row.setData(data);
        PerformanceDatasetRowEntity saved = rowRepository.save(row);
        refreshDatasetStats(dataset);
        return toRowDto(saved);
    }

    @Transactional
    public void deactivateRow(Long datasetId, Long rowId) {
        PerformanceDatasetEntity dataset = activeDataset(datasetId);
        PerformanceDatasetRowEntity row = rowRepository.findById(rowId)
                .orElseThrow(() -> notFound("Dataset row not found: " + rowId));
        if (!datasetId.equals(row.getDatasetId()) || !Boolean.TRUE.equals(row.getActive())) {
            throw notFound("Dataset row not found: " + rowId);
        }
        row.setActive(false);
        rowRepository.save(row);
        refreshDatasetStats(dataset);
    }

    @Transactional
    public PerformanceDatasetDto upload(Long projectId, String name, String description, String defaultMappingJson, MultipartFile file) {
        if (projectId == null) {
            throw badRequest("projectId is required.");
        }
        if (name == null || name.isBlank()) {
            throw badRequest("Dataset name is required.");
        }
        if (file == null || file.isEmpty()) {
            throw badRequest("Dataset file is required.");
        }
        Map<String, String> defaultMapping = parseDefaultMapping(defaultMappingJson);
        PerformanceDatasetParser.ParsedDataset parsed;
        try {
            parsed = parser.parse(file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw badRequest("Dataset file could not be read: " + e.getMessage());
        }
        Date now = new Date();
        PerformanceDatasetEntity dataset = new PerformanceDatasetEntity();
        dataset.setProjectId(projectId);
        dataset.setName(name.trim());
        dataset.setDescription(blankToNull(description));
        dataset.setSourceType(parsed.sourceType());
        dataset.setColumnSchema(parsed.columnSchema());
        dataset.setDefaultMapping(defaultMapping);
        dataset.setRowCount(parsed.rows().size());
        dataset.setActive(true);
        dataset.setCreatedAt(now);
        dataset.setUpdatedAt(now);
        PerformanceDatasetEntity saved = datasetRepository.save(dataset);

        for (int index = 0; index < parsed.rows().size(); index++) {
            PerformanceDatasetRowEntity row = new PerformanceDatasetRowEntity();
            row.setDatasetId(saved.getDatasetId());
            row.setRowIndex(index);
            row.setData(parsed.rows().get(index));
            row.setActive(true);
            rowRepository.save(row);
        }
        return toDto(saved);
    }

    PerformanceDatasetDto toDto(PerformanceDatasetEntity entity) {
        return new PerformanceDatasetDto(
                entity.getDatasetId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getDescription(),
                entity.getSourceType(),
                entity.getColumnSchema(),
                entity.getDefaultMapping(),
                entity.getRowCount(),
                entity.getActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    PerformanceDatasetRowDto toRowDto(PerformanceDatasetRowEntity entity) {
        return new PerformanceDatasetRowDto(
                entity.getRowId(),
                entity.getDatasetId(),
                entity.getRowIndex(),
                entity.getData(),
                entity.getActive()
        );
    }

    private void validateDatasetRequest(PerformanceDatasetRequest request) {
        if (request == null) {
            throw badRequest("Dataset request is required.");
        }
        if (request.getProjectId() == null) {
            throw badRequest("projectId is required.");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw badRequest("Dataset name is required.");
        }
    }

    private Map<String, Object> validateRowRequest(PerformanceDatasetRowRequest request) {
        if (request == null || request.getData() == null || request.getData().isEmpty()) {
            throw badRequest("Dataset row data is required.");
        }
        return new LinkedHashMap<>(request.getData());
    }

    private PerformanceDatasetEntity activeDataset(Long datasetId) {
        if (datasetId == null) {
            throw badRequest("datasetId is required.");
        }
        PerformanceDatasetEntity entity = datasetRepository.findById(datasetId)
                .orElseThrow(() -> notFound("Dataset not found: " + datasetId));
        if (!Boolean.TRUE.equals(entity.getActive())) {
            throw notFound("Dataset not found: " + datasetId);
        }
        return entity;
    }

    private void refreshDatasetStats(PerformanceDatasetEntity dataset) {
        List<PerformanceDatasetRowEntity> rows = rowRepository.findByDatasetIdAndActiveTrueOrderByRowIndexAsc(dataset.getDatasetId());
        dataset.setRowCount(rows.size());
        dataset.setColumnSchema(schemaFromRows(rows));
        dataset.setUpdatedAt(new Date());
        datasetRepository.save(dataset);
    }

    private Map<String, Object> schemaFromRows(List<PerformanceDatasetRowEntity> rows) {
        Map<String, Object> schema = new LinkedHashMap<>();
        for (PerformanceDatasetRowEntity row : rows) {
            Map<String, Object> data = row.getData();
            if (data == null) {
                continue;
            }
            data.forEach((key, value) -> schema.putIfAbsent(key, typeName(value)));
        }
        return schema;
    }

    private Map<String, Object> schemaFromMapping(Map<String, String> mapping) {
        Map<String, Object> schema = new LinkedHashMap<>();
        if (mapping != null) {
            mapping.values().stream()
                    .filter(value -> value != null && !value.isBlank())
                    .forEach(value -> schema.putIfAbsent(value, "string"));
        }
        return schema;
    }

    private Map<String, String> parseDefaultMapping(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, String> parsed = objectMapper.readValue(raw, new TypeReference<LinkedHashMap<String, String>>() {
            });
            return safeMapping(parsed);
        } catch (JsonProcessingException e) {
            throw badRequest("defaultMapping must be a JSON object with string values.");
        }
    }

    private Map<String, String> safeMapping(Map<String, String> mapping) {
        Map<String, String> safe = new LinkedHashMap<>();
        if (mapping == null) {
            return safe;
        }
        mapping.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                safe.put(key, value);
            }
        });
        return safe;
    }

    private String typeName(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof List<?>) {
            return "array";
        }
        if (value instanceof Map<?, ?>) {
            return "object";
        }
        return "string";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
