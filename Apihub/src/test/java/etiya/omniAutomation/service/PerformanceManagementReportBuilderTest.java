package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAnalysisSummary;
import etiya.omniAutomation.business.dto.PerformanceComparisonMetric;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceManagementReport;
import etiya.omniAutomation.business.dto.PerformanceManagementRiskLevel;
import etiya.omniAutomation.business.dto.PerformanceManagementStepAssessment;
import etiya.omniAutomation.business.dto.PerformanceManagementStepStatus;
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

class PerformanceManagementReportBuilderTest {

    private final PerformanceManagementReportBuilder builder = new PerformanceManagementReportBuilder();

    @Test
    void passedThresholdProducesLowRiskAndBaselineMissingTrend() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_PASSED, 0, 500, 2000, 3000, 25),
                new PerformanceThresholdResult(true, "COMPLETED - PASSED", List.of(), PerformanceThresholdConfig.defaults()),
                analysis("auth"),
                null,
                availableEnvironmentMetrics(),
                null,
                List.of(summary("auth", 500, 2000, 3000, 0))
        );

        assertEquals("Basarili", report.overallStatus());
        assertEquals(PerformanceManagementRiskLevel.LOW, report.riskLevel());
        assertTrue(report.trendSummary().contains("Baseline karsilastirmasi bulunmuyor"));
        assertFalse(report.slaSummary().isEmpty());
        assertNotNull(report.stepAssessmentSummary());
        assertFalse(report.stepAssessments().isEmpty());
        assertNotNull(report.detailExplanation());
    }

    @Test
    void failedPercentilesProduceHighRiskAndPercentileAction() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_FAILED, 0, 900, 6000, 8000, 25),
                new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("P95 threshold exceeded"), PerformanceThresholdConfig.defaults()),
                analysis("createCustomer"),
                new PerformanceErrorAnalysis(0, 0, List.of(), List.of(), null, List.of()),
                availableEnvironmentMetrics(),
                null,
                List.of(summary("createCustomer", 900, 6000, 8000, 0))
        );

        assertEquals("Basarisiz", report.overallStatus());
        assertEquals(PerformanceManagementRiskLevel.HIGH, report.riskLevel());
        assertTrue(report.recommendedActions().contains("Yuku artirmadan once problemli adimdaki yuksek percentile gecikmesini inceleyin."));
        assertEquals("createCustomer", report.problemAreas().get(0).stepName());
    }

    @Test
    void severeP99ProducesCriticalRisk() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_FAILED, 0, 900, 6000, 12000, 25),
                new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("P99 threshold exceeded"), PerformanceThresholdConfig.defaults()),
                analysis("createCustomer"),
                null,
                availableEnvironmentMetrics(),
                null,
                List.of(summary("createCustomer", 900, 6000, 12000, 0))
        );

        assertEquals(PerformanceManagementRiskLevel.CRITICAL, report.riskLevel());
    }

    @Test
    void errorRunProducesCriticalRisk() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.ERROR, 0, 0, 0, 0, 0),
                null,
                null,
                null,
                availableEnvironmentMetrics(),
                null,
                List.of()
        );

        assertEquals("Hata", report.overallStatus());
        assertEquals(PerformanceManagementRiskLevel.CRITICAL, report.riskLevel());
    }

    @Test
    void baselineRegressionProducesAtLeastMediumRiskAndRegressionAction() {
        PerformanceComparisonResult comparison = new PerformanceComparisonResult(1L, 2L, List.of(
                new PerformanceComparisonMetric("p95", 100, 200, 100, "REGRESSED", false),
                new PerformanceComparisonMetric("throughput", 10, 12, 2, "IMPROVED", true)
        ));

        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_PASSED, 0, 500, 2000, 3000, 25),
                new PerformanceThresholdResult(true, "COMPLETED - PASSED", List.of(), PerformanceThresholdConfig.defaults()),
                analysis("auth"),
                null,
                availableEnvironmentMetrics(),
                comparison,
                List.of(summary("auth", 500, 2000, 3000, 0))
        );

        assertEquals(PerformanceManagementRiskLevel.MEDIUM, report.riskLevel());
        assertTrue(report.trendSummary().contains("1 metrik iyilesti, 1 metrik kotulesti"));
        assertTrue(report.recommendedActions().contains("Surumu onaylamadan once kotulesen metrikleri secili baseline ile karsilastirin."));
    }

    @Test
    void missingEnvironmentMetricsProducesEnvironmentAction() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_PASSED, 0, 500, 2000, 3000, 25),
                new PerformanceThresholdResult(true, "COMPLETED - PASSED", List.of(), PerformanceThresholdConfig.defaults()),
                analysis("auth"),
                null,
                unavailableEnvironmentMetrics(),
                null,
                List.of(summary("auth", 500, 2000, 3000, 0))
        );

        assertTrue(report.recommendedActions().contains("Uygulama, veritabani ve altyapi kaynaklarini ayirmak icin ortam metriklerini baglayin."));
    }

    @Test
    void stepExceedingErrorThresholdNeedsImprovementAndUsesErrorReason() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_FAILED, 10, 300, 600, 900, 25),
                new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("Error threshold exceeded"), PerformanceThresholdConfig.defaults()),
                null,
                new PerformanceErrorAnalysis(1, 10, List.of(), List.of(), "error", List.of()),
                availableEnvironmentMetrics(),
                null,
                List.of(summary("payment", 300, 600, 900, 1))
        );

        PerformanceManagementStepAssessment assessment = report.stepAssessments().get(0);
        assertEquals(PerformanceManagementStepStatus.NEEDS_IMPROVEMENT, assessment.status());
        assertEquals(PerformanceManagementRiskLevel.CRITICAL, assessment.priority());
        assertEquals("Hata oranı hedefin üzerinde.", assessment.mainReason());
        assertTrue(assessment.evidence().contains("Hata oranı 10.00%"));
    }

    @Test
    void stepExceedingP95NeedsImprovement() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_FAILED, 0, 500, 4000, 4500, 25),
                new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("P95 threshold exceeded"), PerformanceThresholdConfig.defaults()),
                null,
                null,
                availableEnvironmentMetrics(),
                null,
                List.of(summary("search", 500, 4000, 4500, 0))
        );

        PerformanceManagementStepAssessment assessment = report.stepAssessments().get(0);
        assertEquals(PerformanceManagementStepStatus.NEEDS_IMPROVEMENT, assessment.status());
        assertEquals(PerformanceManagementRiskLevel.HIGH, assessment.priority());
        assertEquals("P95 hedefin üzerinde.", assessment.mainReason());
    }

    @Test
    void nearThresholdStepIsWatch() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_PASSED, 0, 500, 2400, 3000, 25),
                new PerformanceThresholdResult(true, "COMPLETED - PASSED", List.of(), PerformanceThresholdConfig.defaults()),
                null,
                null,
                availableEnvironmentMetrics(),
                null,
                List.of(summary("customerSearch", 500, 2400, 3000, 0))
        );

        PerformanceManagementStepAssessment assessment = report.stepAssessments().get(0);
        assertEquals(PerformanceManagementStepStatus.WATCH, assessment.status());
        assertEquals(PerformanceManagementRiskLevel.MEDIUM, assessment.priority());
        assertEquals("P95 hedefe yakın.", assessment.mainReason());
    }

    @Test
    void healthyStepIsGood() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_PASSED, 0, 300, 600, 900, 25),
                new PerformanceThresholdResult(true, "COMPLETED - PASSED", List.of(), PerformanceThresholdConfig.defaults()),
                null,
                null,
                availableEnvironmentMetrics(),
                null,
                List.of(summary("auth", 300, 600, 900, 0))
        );

        PerformanceManagementStepAssessment assessment = report.stepAssessments().get(0);
        assertEquals(PerformanceManagementStepStatus.GOOD, assessment.status());
        assertEquals(PerformanceManagementRiskLevel.LOW, assessment.priority());
        assertEquals("Bu adım hedeflerin içinde çalışıyor.", assessment.mainReason());
        assertTrue(report.stepAssessmentSummary().contains("tamamı iyi durumda"));
    }

    @Test
    void zeroSampleStepIsWatch() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_PASSED, 0, 300, 600, 900, 25),
                new PerformanceThresholdResult(true, "COMPLETED - PASSED", List.of(), PerformanceThresholdConfig.defaults()),
                null,
                null,
                availableEnvironmentMetrics(),
                null,
                List.of(summaryWithCounts("optionalStep", 0, 0, 0, 0, 0, 0, 0, 0, 25, 0))
        );

        PerformanceManagementStepAssessment assessment = report.stepAssessments().get(0);
        assertEquals(PerformanceManagementStepStatus.WATCH, assessment.status());
        assertEquals("Bu adımda anlamlı trafik oluşmadı.", assessment.mainReason());
        assertEquals("Toplam istek 0", assessment.evidence());
    }

    @Test
    void stepAssessmentSummaryCountsGroups() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_FAILED, 0, 500, 4000, 4500, 25),
                new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("P95 threshold exceeded"), PerformanceThresholdConfig.defaults()),
                null,
                null,
                availableEnvironmentMetrics(),
                null,
                List.of(
                        summary("bad", 500, 4000, 4500, 0),
                        summary("watch", 500, 2400, 3000, 0),
                        summary("good", 300, 600, 900, 0)
                )
        );

        assertEquals("3 adımdan 1 adım iyileştirilmeli, 1 adım izlenmeli, 1 adım iyi durumda.", report.stepAssessmentSummary());
    }

    @Test
    void errorReasonWinsOverLatencyReason() {
        PerformanceManagementReport report = builder.build(
                runSummary(GeneralEnums.PerformanceStatus.COMPLETED_FAILED, 10, 2000, 4000, 7000, 25),
                new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("Multiple threshold failures"), PerformanceThresholdConfig.defaults()),
                null,
                new PerformanceErrorAnalysis(1, 10, List.of(), List.of(), "error", List.of()),
                availableEnvironmentMetrics(),
                null,
                List.of(summary("checkout", 2000, 4000, 7000, 1))
        );

        PerformanceManagementStepAssessment assessment = report.stepAssessments().get(0);
        assertEquals(PerformanceManagementStepStatus.NEEDS_IMPROVEMENT, assessment.status());
        assertEquals("Hata oranı hedefin üzerinde.", assessment.mainReason());
    }

    private PerformanceRunSummary runSummary(
            GeneralEnums.PerformanceStatus status,
            double errorRate,
            double average,
            double p95,
            double p99,
            double throughput
    ) {
        return new PerformanceRunSummary(
                status,
                new Date(0),
                new Date(1000),
                1000,
                1,
                0,
                10,
                errorRate > 0 ? 9 : 10,
                errorRate > 0 ? 1 : 0,
                errorRate,
                throughput,
                average,
                100,
                p99,
                average,
                average,
                p95,
                p99,
                "createCustomer"
        );
    }

    private PerformanceAnalysisSummary analysis(String stepName) {
        return new PerformanceAnalysisSummary(
                GeneralEnums.PerformanceStatus.COMPLETED_FAILED,
                null,
                stepName,
                stepName,
                stepName,
                stepName,
                null,
                stepName,
                "summary",
                List.of()
        );
    }

    private PerformanceSummary summary(String stepName, double average, double p95, double p99, long failures) {
        return summaryWithCounts(
                stepName,
                10,
                10 - failures,
                failures,
                failures > 0 ? 10 : 0,
                average,
                average,
                p95,
                p99,
                20,
                5
        );
    }

    private PerformanceSummary summaryWithCounts(
            String stepName,
            long sampleCount,
            long successCount,
            long failureCount,
            double errorRate,
            double average,
            double p90,
            double p95,
            double p99,
            double throughput,
            double standardDeviation
    ) {
        return new PerformanceSummary(
                stepName,
                p99,
                100,
                average,
                sampleCount,
                successCount,
                failureCount,
                errorRate,
                throughput,
                average,
                p90,
                p95,
                p99,
                standardDeviation,
                failureCount > 0 ? "error" : null,
                null
        );
    }

    private PerformanceEnvironmentMetrics availableEnvironmentMetrics() {
        return new PerformanceEnvironmentMetrics(true, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    private PerformanceEnvironmentMetrics unavailableEnvironmentMetrics() {
        return new PerformanceEnvironmentMetrics(false, "Metrics unavailable", null, null, null, null, null, null, null, null, null, null, null, List.of());
    }
}
