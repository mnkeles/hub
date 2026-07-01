package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAiActionItem;
import etiya.omniAutomation.business.dto.PerformanceAiManagementReport;
import etiya.omniAutomation.business.dto.PerformanceAnomalyLevel;
import etiya.omniAutomation.business.dto.PerformanceBottleneckType;
import etiya.omniAutomation.business.dto.PerformanceInsightReport;
import etiya.omniAutomation.business.dto.PerformanceInsightSeverity;
import etiya.omniAutomation.business.dto.PerformanceManagementReport;
import etiya.omniAutomation.business.dto.PerformanceManagementRiskLevel;
import etiya.omniAutomation.business.dto.PerformanceManagementSlaItem;
import etiya.omniAutomation.business.dto.PerformanceMetricInsight;
import etiya.omniAutomation.business.dto.PerformanceReleaseReadiness;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceAiReportValidatorTest {

    private final PerformanceAiReportValidator validator = new PerformanceAiReportValidator();

    @Test
    void rejectsBlockedReadinessContradiction() {
        PerformanceAiValidationResult result = validator.validate(
                aiReport("Bu sonuc release ready gorunuyor.", action("P1", "P95 incele", null, "P95")),
                managementReport(),
                insightReport(PerformanceReleaseReadiness.BLOCKED),
                null,
                failedThreshold()
        );

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("blocked release readiness")));
    }

    @Test
    void rejectsHealthyLanguageWhenThresholdFailed() {
        PerformanceAiValidationResult result = validator.validate(
                aiReport("P95 yuksek olsa da performans iyi.", action("P1", "P95 incele", null, "P95")),
                managementReport(),
                insightReport(PerformanceReleaseReadiness.CONDITIONAL),
                null,
                failedThreshold()
        );

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("failed threshold")));
    }

    @Test
    void rejectsUnrelatedActionItem() {
        PerformanceAiValidationResult result = validator.validate(
                aiReport("P95 incelenmeli.", action("P1", "Baska alan", "unknownStep", "CPU")),
                managementReport(),
                insightReport(PerformanceReleaseReadiness.CONDITIONAL),
                null,
                failedThreshold()
        );

        assertFalse(result.valid());
        assertTrue(result.errors().stream().anyMatch(error -> error.contains("not tied")));
    }

    @Test
    void acceptsActionTiedToProblemMetric() {
        PerformanceAiValidationResult result = validator.validate(
                aiReport("P95 incelenmeli.", action("P1", "P95 incele", null, "P95")),
                managementReport(),
                insightReport(PerformanceReleaseReadiness.CONDITIONAL),
                null,
                failedThreshold()
        );

        assertTrue(result.valid());
    }

    private PerformanceAiManagementReport aiReport(String narrative, PerformanceAiActionItem actionItem) {
        return new PerformanceAiManagementReport(
                true,
                new Date(),
                "fake-model",
                narrative,
                "Teknik analiz P95 sinyalini dogruluyor.",
                "Root cause ihtimali.",
                List.of(actionItem),
                "Kosullu ilerlenebilir.",
                List.of(),
                null,
                PerformanceReportVersions.AI_REPORT_SCHEMA_VERSION,
                PerformanceReportVersions.AI_REPORT_GENERATOR_VERSION,
                10L,
                1,
                null,
                List.of(),
                "promptHash",
                "inputHash",
                100,
                null,
                null,
                null
        );
    }

    private PerformanceAiActionItem action(String priority, String title, String stepName, String metric) {
        return new PerformanceAiActionItem(priority, title, "Aksiyon aciklamasi.", stepName, metric);
    }

    private PerformanceManagementReport managementReport() {
        return new PerformanceManagementReport(
                "Basarisiz",
                PerformanceManagementRiskLevel.HIGH,
                "P95 iyilestirilmeli.",
                List.of(),
                "Executive summary",
                List.of(new PerformanceManagementSlaItem("P95", false, "<= 3000 ms", "4000 ms", "P95 failed")),
                List.of(),
                "Baseline yok",
                List.of(),
                "Detay"
        );
    }

    private PerformanceInsightReport insightReport(PerformanceReleaseReadiness readiness) {
        return new PerformanceInsightReport(
                80,
                PerformanceAnomalyLevel.ANOMALOUS,
                null,
                false,
                0.7,
                false,
                60,
                PerformanceBottleneckType.LATENCY,
                readiness,
                List.of(),
                List.of(new PerformanceMetricInsight("P95", PerformanceInsightSeverity.HIGH, "4000 ms", "<= 3000 ms", "P95 high")),
                List.of(),
                PerformanceReportVersions.INSIGHT_SCHEMA_VERSION,
                PerformanceReportVersions.INSIGHT_GENERATOR_VERSION
        );
    }

    private PerformanceThresholdResult failedThreshold() {
        return new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("P95 failed"), PerformanceThresholdConfig.defaults());
    }
}
