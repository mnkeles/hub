package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceErrorTypeCount;
import etiya.omniAutomation.business.dto.PerformanceExportPayload;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceStepErrorCount;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.business.dto.PerformanceThresholdPreset;
import etiya.omniAutomation.business.dto.PerformanceValidationChecklist;
import etiya.omniAutomation.business.dto.PerformanceValidationChecklistItem;
import etiya.omniAutomation.business.dto.PerformanceValidationStatus;
import etiya.omniAutomation.common.GeneralEnums;
import etiya.omniAutomation.entity.PerfRsltEntity;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceExportServiceTest {

    private final PerformanceExportService service = new PerformanceExportService(new PerformanceManagementReportBuilder());

    @Test
    void buildsJsonPayloadFromEntityAndThreadDetail() {
        PerfRsltEntity entity = entity();
        PerformanceThreadGroup detail = new PerformanceThreadGroup(List.of());

        PerformanceExportPayload payload = service.buildPayload(entity, detail);

        assertEquals(entity.getRunSummary(), payload.runSummary());
        assertEquals(entity.getThresholdResult(), payload.thresholdResult());
        assertNotNull(payload.managementReport());
        assertEquals(detail, payload.threadDetail());
    }

    @Test
    void buildsCsvWithRunThresholdStepAndErrorSections() {
        String csv = service.buildCsv(service.buildPayload(entity(), new PerformanceThreadGroup(List.of())));

        assertTrue(csv.contains("Run Summary"));
        assertTrue(csv.contains("Report Metadata"));
        assertTrue(csv.contains("Validation Checklist"));
        assertTrue(csv.contains("run_completed"));
        assertTrue(csv.contains("Threshold Result"));
        assertTrue(csv.contains("Step Summary"));
        assertTrue(csv.contains("Error Summary"));
        assertTrue(csv.contains("P95"));
    }

    @Test
    void escapesCsvCellsWithCommaAndQuotes() {
        PerfRsltEntity entity = entity();
        entity.setSummary(List.of(new PerformanceSummary("step, \"quoted\"", 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, "error, value", null)));

        String csv = service.buildCsv(service.buildPayload(entity, new PerformanceThreadGroup(List.of())));

        assertTrue(csv.contains("\"step, \"\"quoted\"\"\""));
        assertTrue(csv.contains("\"error, value\""));
    }

    private PerfRsltEntity entity() {
        PerfRsltEntity entity = new PerfRsltEntity();
        entity.setRunSummary(new PerformanceRunSummary(GeneralEnums.PerformanceStatus.COMPLETED, new Date(0), new Date(1000), 1000,
                1, 0, 10, 9, 1, 10, 20, 100, 10, 500, 50, 90, 95, 99, "step"));
        entity.setResultSchemaVersion(1);
        entity.setThresholdPreset(PerformanceThresholdPreset.NORMAL);
        entity.setThresholdConfig(PerformanceThresholdConfig.defaults());
        entity.setBaseline(false);
        entity.setThresholdResult(new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("reason"), PerformanceThresholdConfig.defaults()));
        entity.setValidationChecklist(new PerformanceValidationChecklist(
                List.of(new PerformanceValidationChecklistItem("run_completed", "Run completed", PerformanceValidationStatus.PASSED, "done")),
                null,
                null
        ));
        entity.setSummary(List.of(new PerformanceSummary("step", 500, 10, 100, 10, 9, 1, 10, 20, 50, 90, 95, 99, 5, "last", null)));
        entity.setErrorAnalysis(new PerformanceErrorAnalysis(1, 10, List.of(new PerformanceErrorTypeCount("HTTP 5xx", 1)),
                List.of(new PerformanceStepErrorCount("step", 1)), "last", List.of()));
        return entity;
    }
}
