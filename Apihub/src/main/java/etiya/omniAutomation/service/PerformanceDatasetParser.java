package etiya.omniAutomation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.PerformanceDatasetSourceType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class PerformanceDatasetParser {

    private final ObjectMapper objectMapper;

    public PerformanceDatasetParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedDataset parse(String fileName, byte[] content) {
        if (fileName == null || fileName.isBlank()) {
            throw badRequest("Dataset file name is required.");
        }
        if (content == null || content.length == 0) {
            throw badRequest("Dataset file is empty.");
        }
        String normalized = fileName.toLowerCase(Locale.ROOT);
        if (normalized.endsWith(".csv")) {
            return parseCsv(new String(content, StandardCharsets.UTF_8));
        }
        if (normalized.endsWith(".json")) {
            return parseJson(content);
        }
        throw badRequest("Invalid dataset file type. Only CSV and JSON files are accepted.");
    }

    private ParsedDataset parseCsv(String content) {
        List<List<String>> records = parseCsvRecords(content);
        if (records.isEmpty()) {
            throw badRequest("CSV header is required.");
        }
        List<String> headers = records.get(0).stream()
                .map(String::trim)
                .toList();
        validateHeaders(headers);
        if (records.size() < 2) {
            throw badRequest("CSV must contain at least one data row.");
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < records.size(); rowIndex++) {
            List<String> record = records.get(rowIndex);
            if (record.stream().allMatch(value -> value == null || value.isBlank())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                String value = columnIndex < record.size() ? record.get(columnIndex) : "";
                row.put(headers.get(columnIndex), value);
            }
            rows.add(row);
        }
        if (rows.isEmpty()) {
            throw badRequest("CSV must contain at least one non-empty data row.");
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        headers.forEach(header -> schema.put(header, "string"));
        return new ParsedDataset(PerformanceDatasetSourceType.CSV, schema, rows);
    }

    private List<List<String>> parseCsvRecords(String content) {
        List<List<String>> records = new ArrayList<>();
        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < content.length(); index++) {
            char current = content.charAt(index);
            if (quoted) {
                if (current == '"') {
                    if (index + 1 < content.length() && content.charAt(index + 1) == '"') {
                        currentValue.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    currentValue.append(current);
                }
                continue;
            }

            if (current == '"') {
                quoted = true;
            } else if (current == ',') {
                currentRecord.add(currentValue.toString());
                currentValue.setLength(0);
            } else if (current == '\n') {
                currentRecord.add(trimTrailingCarriageReturn(currentValue.toString()));
                currentValue.setLength(0);
                records.add(currentRecord);
                currentRecord = new ArrayList<>();
            } else {
                currentValue.append(current);
            }
        }
        if (quoted) {
            throw badRequest("CSV contains an unclosed quoted value.");
        }
        currentRecord.add(trimTrailingCarriageReturn(currentValue.toString()));
        if (!(currentRecord.size() == 1 && currentRecord.get(0).isBlank() && records.isEmpty())) {
            records.add(currentRecord);
        }
        return records;
    }

    private String trimTrailingCarriageReturn(String value) {
        return value.endsWith("\r") ? value.substring(0, value.length() - 1) : value;
    }

    private void validateHeaders(List<String> headers) {
        if (headers.isEmpty() || headers.stream().anyMatch(String::isBlank)) {
            throw badRequest("CSV header names cannot be empty.");
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String header : headers) {
            if (!seen.add(header)) {
                throw badRequest("CSV header contains duplicate field: " + header);
            }
        }
    }

    private ParsedDataset parseJson(byte[] content) {
        try {
            JsonNode root = objectMapper.readTree(content);
            JsonNode rowsNode = root.isArray() ? root : root.get("rows");
            if (rowsNode == null || !rowsNode.isArray()) {
                throw badRequest("JSON dataset must be an array of objects or an object with a rows array.");
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            Map<String, Object> schema = new LinkedHashMap<>();
            for (JsonNode rowNode : rowsNode) {
                if (!rowNode.isObject()) {
                    throw badRequest("JSON dataset rows must be objects.");
                }
                Map<String, Object> row = objectMapper.convertValue(rowNode, new TypeReference<LinkedHashMap<String, Object>>() {
                });
                if (row.isEmpty()) {
                    throw badRequest("JSON dataset rows cannot be empty objects.");
                }
                row.forEach((key, value) -> schema.putIfAbsent(key, typeName(value)));
                rows.add(row);
            }
            if (rows.isEmpty()) {
                throw badRequest("JSON dataset must contain at least one row.");
            }
            return new ParsedDataset(PerformanceDatasetSourceType.JSON, schema, rows);
        } catch (IOException e) {
            throw badRequest("JSON dataset could not be parsed: " + e.getMessage());
        }
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

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record ParsedDataset(
            PerformanceDatasetSourceType sourceType,
            Map<String, Object> columnSchema,
            List<Map<String, Object>> rows
    ) {
    }
}
