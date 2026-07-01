package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceErrorTypeCount;
import etiya.omniAutomation.business.dto.PerformanceExportPayload;
import etiya.omniAutomation.business.dto.PerformanceAiActionItem;
import etiya.omniAutomation.business.dto.PerformanceAiManagementReport;
import etiya.omniAutomation.business.dto.PerformanceAnomalyLevel;
import etiya.omniAutomation.business.dto.PerformanceBottleneckType;
import etiya.omniAutomation.business.dto.PerformanceInsightReport;
import etiya.omniAutomation.business.dto.PerformanceInsightSeverity;
import etiya.omniAutomation.business.dto.PerformanceMetricInsight;
import etiya.omniAutomation.business.dto.PerformanceReleaseReadiness;
import etiya.omniAutomation.business.dto.PerformanceRootCauseHint;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceExportServiceTest {

    private final PerformanceExportService service = new PerformanceExportService(
            new PerformanceManagementReportBuilder(),
            new PerformanceInsightBuilder()
    );

    @Test
    void buildsJsonPayloadFromEntityAndThreadDetail() {
        PerfRsltEntity entity = entity();
        PerformanceThreadGroup detail = new PerformanceThreadGroup(List.of());

        PerformanceExportPayload payload = service.buildPayload(entity, detail);

        assertEquals(entity.getRunSummary(), payload.runSummary());
        assertEquals(entity.getThresholdResult(), payload.thresholdResult());
        assertNotNull(payload.managementReport());
        assertEquals(entity.getInsightReport(), payload.insightReport());
        assertSame(entity.getInsightReport(), payload.insightReport());
        assertEquals(entity.getAiManagementReport(), payload.aiManagementReport());
        assertEquals(detail, payload.threadDetail());
    }

    @Test
    void buildsInsightAndAiFallbackForLegacyEntity() {
        PerfRsltEntity entity = entity();
        entity.setInsightReport(null);
        entity.setAiManagementReport(null);

        PerformanceExportPayload payload = service.buildPayload(entity, new PerformanceThreadGroup(List.of()));

        assertNotNull(payload.managementReport());
        assertNotNull(payload.insightReport());
        assertNotNull(payload.aiManagementReport());
        assertFalse(payload.aiManagementReport().generated());
        assertEquals("AI report is not available for this performance result.", payload.aiManagementReport().errorMessage());
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
        assertTrue(csv.contains("Decision Summary"));
        assertTrue(csv.contains("Metric Insights"));
        assertTrue(csv.contains("Root Cause Hints"));
        assertTrue(csv.contains("AI Action Plan"));
        assertTrue(csv.contains("AI Observability"));
        assertTrue(csv.contains("P95 yuksek"));
        assertTrue(csv.contains("Slow SQL"));
        assertTrue(csv.contains("P95 incele"));
    }

    @Test
    void buildsCsvWhenInsightAndAiMetadataAreMissing() {
        PerfRsltEntity entity = entity();
        entity.setInsightReport(null);
        entity.setAiManagementReport(null);

        String csv = service.buildCsv(service.buildPayload(entity, new PerformanceThreadGroup(List.of())));

        assertTrue(csv.contains("Decision Summary"));
        assertTrue(csv.contains("AI Observability"));
    }

    @Test
    void csvIncludesAiObservabilityWhenFallback() {
        PerfRsltEntity entity = entity();
        entity.setAiManagementReport(PerformanceAiManagementReport.notGenerated("not generated"));

        String csv = service.buildCsv(service.buildPayload(entity, new PerformanceThreadGroup(List.of())));

        assertTrue(csv.contains("AI Observability"));
        assertTrue(csv.contains("not generated"));
        assertTrue(csv.contains("performance-ai-report-v2"));
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
        entity.setInsightReport(new PerformanceInsightReport(
                0,
                PerformanceAnomalyLevel.NORMAL,
                null,
                false,
                null,
                false,
                0,
                PerformanceBottleneckType.NONE,
                PerformanceReleaseReadiness.UNKNOWN,
                List.of(new PerformanceRootCauseHint(
                        PerformanceInsightSeverity.WARNING,
                        "DATABASE",
                        "Slow SQL",
                        "Slow query sinyali var.",
                        "Sorgulari inceleyin."
                )),
                List.of(new PerformanceMetricInsight(
                        "P95",
                        PerformanceInsightSeverity.HIGH,
                        "4000 ms",
                        "<= 3000 ms",
                        "P95 yuksek"
                )),
                List.of(),
                PerformanceReportVersions.INSIGHT_SCHEMA_VERSION,
                PerformanceReportVersions.INSIGHT_GENERATOR_VERSION
        ));
        entity.setAiManagementReport(new PerformanceAiManagementReport(
                true,
                new Date(),
                "fake-model",
                "Executive",
                "Technical",
                "Root cause",
                List.of(new PerformanceAiActionItem("P1", "P95 incele", "Yavas step incelenmeli.", "step", "P95")),
                "Readiness",
                List.of(),
                null,
                PerformanceReportVersions.AI_REPORT_SCHEMA_VERSION,
                PerformanceReportVersions.AI_REPORT_GENERATOR_VERSION,
                12L,
                1,
                null,
                List.of(),
                "promptHash",
                "inputHash",
                100,
                null,
                null,
                null
        ));
        return entity;
    }
}
