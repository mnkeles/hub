package etiya.omniAutomation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.PerformanceDatasetSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PerformanceDatasetParserTest {

    private final PerformanceDatasetParser parser = new PerformanceDatasetParser(new ObjectMapper());

    @Test
    void parseCsvWithQuotedValues() {
        String csv = "id,name,city\n1,\"Ada, Lovelace\",London\n2,Grace,Arlington";

        PerformanceDatasetParser.ParsedDataset parsed = parser.parse("users.csv", csv.getBytes(StandardCharsets.UTF_8));

        assertEquals(PerformanceDatasetSourceType.CSV, parsed.sourceType());
        assertEquals("string", parsed.columnSchema().get("name"));
        assertEquals(2, parsed.rows().size());
        assertEquals("Ada, Lovelace", parsed.rows().get(0).get("name"));
    }

    @Test
    void rejectCsvDuplicateHeader() {
        String csv = "id,id\n1,2";

        assertThrows(ResponseStatusException.class, () -> parser.parse("bad.csv", csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parseJsonArray() {
        String json = "[{\"id\":1,\"active\":true,\"name\":\"Ada\"}]";

        PerformanceDatasetParser.ParsedDataset parsed = parser.parse("users.json", json.getBytes(StandardCharsets.UTF_8));

        assertEquals(PerformanceDatasetSourceType.JSON, parsed.sourceType());
        assertEquals("number", parsed.columnSchema().get("id"));
        assertEquals("boolean", parsed.columnSchema().get("active"));
        assertEquals("string", parsed.columnSchema().get("name"));
    }

    @Test
    void parseJsonRowsObject() {
        String json = "{\"rows\":[{\"id\":1,\"name\":\"Ada\"}]}";

        PerformanceDatasetParser.ParsedDataset parsed = parser.parse("users.json", json.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, parsed.rows().size());
        assertEquals("Ada", parsed.rows().get(0).get("name"));
    }

    @Test
    void rejectJsonScalarRows() {
        String json = "[1,2,3]";

        assertThrows(ResponseStatusException.class, () -> parser.parse("bad.json", json.getBytes(StandardCharsets.UTF_8)));
    }
}
