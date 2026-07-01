package etiya.omniAutomation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.PerformanceAiManagementReport;
import etiya.omniAutomation.business.dto.PerformanceAnomalyLevel;
import etiya.omniAutomation.business.dto.PerformanceBottleneckType;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceFailedRequest;
import etiya.omniAutomation.business.dto.PerformanceInsightReport;
import etiya.omniAutomation.business.dto.PerformanceInsightSeverity;
import etiya.omniAutomation.business.dto.PerformanceManagementReport;
import etiya.omniAutomation.business.dto.PerformanceManagementSlaItem;
import etiya.omniAutomation.business.dto.PerformanceManagementStepAssessment;
import etiya.omniAutomation.business.dto.PerformanceManagementRiskLevel;
import etiya.omniAutomation.business.dto.PerformanceManagementStepStatus;
import etiya.omniAutomation.business.dto.PerformanceMetricInsight;
import etiya.omniAutomation.business.dto.PerformanceReleaseReadiness;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.common.GeneralEnums;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceAiReportServiceTest {

    @Test
    void parsesValidAiJson() {
        FakePerformanceAiClient client = new FakePerformanceAiClient(validJson());
        PerformanceAiReportService service = service(client);

        PerformanceAiManagementReport report = service.generate(
                managementReport(),
                insightReport(PerformanceReleaseReadiness.CONDITIONAL),
                runSummary(),
                thresholdResult(),
                null,
                null,
                null,
                null,
                List.of(stepSummary("slowStep", 6000))
        );

        assertTrue(report.generated());
        assertEquals("fake-model", report.model());
        assertEquals("Yonetim ozeti", report.executiveNarrative());
        assertEquals(1, report.recommendedActionPlan().size());
        assertEquals(PerformanceReportVersions.AI_REPORT_SCHEMA_VERSION, report.schemaVersion());
        assertEquals(PerformanceReportVersions.AI_REPORT_GENERATOR_VERSION, report.generatedByVersion());
        assertNotNull(client.lastSystemPrompt);
        assertTrue(client.lastUserPrompt.contains("slowStep"));
    }

    @Test
    void returnsFallbackWhenAiThrows() {
        FakePerformanceAiClient client = new FakePerformanceAiClient(validJson());
        client.throwOnComplete = true;
        PerformanceAiReportService service = service(client);

        PerformanceAiManagementReport report = service.generate(
                managementReport(),
                insightReport(PerformanceReleaseReadiness.CONDITIONAL),
                runSummary(),
                thresholdResult(),
                null,
                null,
                null,
                null,
                List.of()
        );

        assertFalse(report.generated());
        assertTrue(report.errorMessage().startsWith("AI report generation failed:"));
        assertEquals(PerformanceReportVersions.AI_REPORT_SCHEMA_VERSION, report.schemaVersion());
        assertEquals(PerformanceReportVersions.AI_REPORT_GENERATOR_VERSION, report.generatedByVersion());
    }

    @Test
    void returnsFallbackWhenJsonCannotBeParsed() {
        PerformanceAiReportService service = service(new FakePerformanceAiClient("not-json"));

        PerformanceAiManagementReport report = service.generate(
                managementReport(),
                insightReport(PerformanceReleaseReadiness.CONDITIONAL),
                runSummary(),
                thresholdResult(),
                null,
                null,
                null,
                null,
                List.of()
        );

        assertFalse(report.generated());
        assertEquals("AI response could not be parsed as JSON.", report.errorMessage());
    }

    @Test
    void returnsFallbackForInvalidActionPriorities() {
        String json = """
                {
                  "executiveNarrative": "Yonetim ozeti",
                  "technicalNarrative": "Teknik ozet",
                  "rootCauseNarrative": "Root cause ihtimali",
                  "recommendedActionPlan": [
                    {"priority": "P0", "title": "Gecerli", "description": "Incele", "relatedStepName": "a", "relatedMetric": "P95"},
                    {"priority": "P9", "title": "Gecersiz", "description": "Atla", "relatedStepName": "b", "relatedMetric": "P99"}
                  ],
                  "releaseReadinessNarrative": "Kosullu ilerlenebilir",
                  "limitations": []
                }
                """;
        PerformanceAiReportService service = service(new FakePerformanceAiClient(json));

        PerformanceAiManagementReport report = service.generate(
                managementReport(),
                insightReport(PerformanceReleaseReadiness.CONDITIONAL),
                runSummary(),
                thresholdResult(),
                null,
                null,
                null,
                null,
                List.of()
        );

        assertFalse(report.generated());
        assertEquals("VALIDATION_FAILED", report.failureReason());
        assertTrue(report.validationErrors().stream().anyMatch(error -> error.contains("Invalid action priority: P9")));
    }

    @Test
    void rejectsReadinessContradiction() {
        String json = """
                {
                  "executiveNarrative": "Yonetim ozeti",
                  "technicalNarrative": "Teknik ozet",
                  "rootCauseNarrative": "Root cause ihtimali",
                  "recommendedActionPlan": [],
                  "releaseReadinessNarrative": "Bu sonuc release ready kabul edilebilir.",
                  "limitations": []
                }
                """;
        PerformanceAiReportService service = service(new FakePerformanceAiClient(json));

        PerformanceAiManagementReport report = service.generate(
                managementReport(),
                insightReport(PerformanceReleaseReadiness.BLOCKED),
                runSummary(),
                thresholdResult(),
                null,
                null,
                null,
                null,
                List.of()
        );

        assertFalse(report.generated());
        assertEquals("AI output failed validation.", report.errorMessage());
        assertEquals("VALIDATION_FAILED", report.failureReason());
    }

    @Test
    void doesNotSendRawThreadDetail() {
        String rawErrorBody = "RAW_RESPONSE_BODY_" + "x".repeat(600);
        FakePerformanceAiClient client = new FakePerformanceAiClient(validJson());
        PerformanceAiReportService service = service(client);

        PerformanceAiManagementReport report = service.generate(
                managementReport(),
                insightReport(PerformanceReleaseReadiness.CONDITIONAL),
                runSummary(),
                thresholdResult(),
                null,
                new PerformanceErrorAnalysis(
                        1,
                        10,
                        List.of(),
                        List.of(),
                        rawErrorBody,
                        List.of(new PerformanceFailedRequest(1, "slowStep", 1200.0, "HTTP_500", rawErrorBody))
                ),
                null,
                null,
                List.of(stepSummary("slowStep", 6000))
        );

        assertTrue(report.generated());
        assertNotNull(client.lastUserPrompt);
        assertFalse(client.lastUserPrompt.contains(rawErrorBody));
    }

    @Test
    void recordsPromptObservabilityMetadata() {
        PerformanceAiManagementReport report = service(new FakePerformanceAiClient(validJson())).generate(
                managementReport(),
                insightReport(PerformanceReleaseReadiness.CONDITIONAL),
                runSummary(),
                thresholdResult(),
                null,
                null,
                null,
                null,
                List.of(stepSummary("slowStep", 6000))
        );

        assertTrue(report.generated());
        assertNotNull(report.durationMs());
        assertEquals(1, report.attemptCount());
        assertNotNull(report.promptHash());
        assertNotNull(report.inputSummaryHash());
        assertTrue(report.responseSize() > 0);
    }

    @Test
    void legacyFallbackCarriesMetadata() {
        PerformanceAiManagementReport report = PerformanceAiManagementReport.notGenerated("legacy unavailable");

        assertFalse(report.generated());
        assertEquals(PerformanceReportVersions.AI_REPORT_SCHEMA_VERSION, report.schemaVersion());
        assertEquals(PerformanceReportVersions.AI_REPORT_GENERATOR_VERSION, report.generatedByVersion());
        assertEquals(1, report.attemptCount());
        assertTrue(report.validationErrors().isEmpty());
    }

    private static String validJson() {
        return """
                {
                  "executiveNarrative": "Yonetim ozeti",
                  "technicalNarrative": "Teknik ozet",
                  "rootCauseNarrative": "Root cause ihtimali",
                  "recommendedActionPlan": [
                    {"priority": "P1", "title": "P95 incele", "description": "Yavas step analiz edilmeli.", "relatedStepName": "slowStep", "relatedMetric": "P95"}
                  ],
                  "releaseReadinessNarrative": "Kosullu ilerlenebilir",
                  "limitations": ["Ortam metrikleri sinirli"]
                }
                """;
    }

    private PerformanceManagementReport managementReport() {
        return new PerformanceManagementReport(
                "Basarisiz",
                PerformanceManagementRiskLevel.HIGH,
                "1 adim iyilestirilmeli.",
                List.of(new PerformanceManagementStepAssessment(
                        "slowStep",
                        PerformanceManagementStepStatus.NEEDS_IMPROVEMENT,
                        PerformanceManagementRiskLevel.HIGH,
                        "P95 yuksek",
                        "P95 threshold exceeded",
                        "Gecikme",
                        "slowStep P95 incelenmeli",
                        10,
                        9,
                        1,
                        10,
                        1200,
                        3000,
                        4000,
                        6000,
                        15,
                        null
                )),
                "Executive summary",
                List.of(new PerformanceManagementSlaItem("P95", false, "<= 3000 ms", "4000 ms", "P95 threshold exceeded")),
                List.of(),
                "Baseline yok",
                List.of("P95 incele"),
                "Detay"
        );
    }

    private PerformanceInsightReport insightReport(PerformanceReleaseReadiness readiness) {
        return new PerformanceInsightReport(
                readiness == PerformanceReleaseReadiness.BLOCKED ? 90 : 40,
                readiness == PerformanceReleaseReadiness.BLOCKED ? PerformanceAnomalyLevel.CRITICAL : PerformanceAnomalyLevel.WATCH,
                null,
                false,
                0.8,
                false,
                60,
                PerformanceBottleneckType.LATENCY,
                readiness,
                List.of(),
                List.of(new PerformanceMetricInsight(
                        "P95",
                        PerformanceInsightSeverity.HIGH,
                        "4000 ms",
                        "<= 3000 ms",
                        "P95 hedefin uzerinde."
                )),
                List.of(),
                PerformanceReportVersions.INSIGHT_SCHEMA_VERSION,
                PerformanceReportVersions.INSIGHT_GENERATOR_VERSION
        );
    }

    private PerformanceAiReportService service(FakePerformanceAiClient client) {
        return new PerformanceAiReportService(client, new ObjectMapper(), new PerformanceAiReportValidator());
    }

    private PerformanceRunSummary runSummary() {
        return new PerformanceRunSummary(
                GeneralEnums.PerformanceStatus.COMPLETED_FAILED,
                new Date(0),
                new Date(1000),
                1000,
                1,
                0,
                10,
                9,
                1,
                10,
                15,
                1200,
                100,
                6000,
                500,
                1000,
                4000,
                6000,
                "slowStep"
        );
    }

    private PerformanceThresholdResult thresholdResult() {
        return new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("P95 threshold exceeded"), PerformanceThresholdConfig.defaults());
    }

    private PerformanceSummary stepSummary(String stepName, double p99) {
        return new PerformanceSummary(
                stepName,
                p99,
                100,
                1200,
                10,
                9,
                1,
                10,
                15,
                1000,
                2000,
                4000,
                p99,
                100,
                "raw error should not be sent",
                null
        );
    }

    private static final class FakePerformanceAiClient implements PerformanceAiClient {
        private final String response;
        private boolean throwOnComplete;
        private String lastSystemPrompt;
        private String lastUserPrompt;

        private FakePerformanceAiClient(String response) {
            this.response = response;
        }

        @Override
        public String complete(String systemPrompt, String userPrompt) {
            this.lastSystemPrompt = systemPrompt;
            this.lastUserPrompt = userPrompt;
            if (throwOnComplete) {
                throw new IllegalStateException("provider unavailable");
            }
            return response;
        }

        @Override
        public String modelName() {
            return "fake-model";
        }
    }
}
