package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAnomalyLevel;
import etiya.omniAutomation.business.dto.PerformanceBottleneckType;
import etiya.omniAutomation.business.dto.PerformanceComparisonMetric;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceInsightReport;
import etiya.omniAutomation.business.dto.PerformanceReleaseReadiness;
import etiya.omniAutomation.business.dto.PerformanceResponseTimeBuckets;
import etiya.omniAutomation.business.dto.PerformanceResultItemDto;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThread;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.common.GeneralEnums;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceInsightBuilderTest {

    private final PerformanceInsightBuilder builder = new PerformanceInsightBuilder();

    @Test
    void calculatesApdexFromThreadDetail() {
        PerformanceInsightReport report = builder.build(
                passingRun(),
                passedThreshold(),
                null,
                null,
                null,
                null,
                List.of(),
                new PerformanceThreadGroup(List.of(new PerformanceThread(0, List.of(
                        sample(100),
                        sample(900),
                        sample(1500),
                        sample(5000)
                ))))
        );

        assertEquals(0.625, report.apdexScore(), 0.001);
        assertFalse(report.apdexEstimated());
    }

    @Test
    void estimatesApdexFromBucketsWhenSamplesAreMissing() {
        PerformanceSummary summary = new PerformanceSummary(
                "step",
                4000,
                100,
                1000,
                10,
                10,
                0,
                0,
                10,
                800,
                1500,
                2500,
                4000,
                100,
                null,
                new PerformanceResponseTimeBuckets(2, 2, 4, 2)
        );

        PerformanceInsightReport report = builder.build(
                passingRun(),
                passedThreshold(),
                null,
                null,
                null,
                null,
                List.of(summary),
                null
        );

        assertEquals(0.6, report.apdexScore(), 0.001);
        assertTrue(report.apdexEstimated());
    }

    @Test
    void calculatesSloCompliance() {
        PerformanceRunSummary runSummary = new PerformanceRunSummary(
                GeneralEnums.PerformanceStatus.COMPLETED,
                new Date(0),
                new Date(1000),
                1000,
                1,
                0,
                10,
                9,
                1,
                2,
                25,
                800,
                100,
                6000,
                500,
                900,
                4000,
                6000,
                "step"
        );

        PerformanceInsightReport report = builder.build(runSummary, passedThreshold(), null, null, null, null, List.of(), null);

        assertEquals(40.0, report.sloCompliancePercent(), 0.001);
    }

    @Test
    void calculatesRegressionScore() {
        PerformanceComparisonResult comparison = new PerformanceComparisonResult(1L, 2L, List.of(
                new PerformanceComparisonMetric("p99", 100, 200, 100, "REGRESSED", false),
                new PerformanceComparisonMetric("p95", 100, 200, 100, "REGRESSED", false),
                new PerformanceComparisonMetric("throughput", 20, 10, -10, "REGRESSED", false),
                new PerformanceComparisonMetric("totalSamples", 10, 20, 10, "CHANGED", null)
        ));

        PerformanceInsightReport report = builder.build(passingRun(), passedThreshold(), null, null, null, comparison, List.of(), null);

        assertTrue(report.regressionAvailable());
        assertEquals(40.0, report.regressionScore(), 0.001);
    }

    @Test
    void marksRegressionUnavailableWithoutBaseline() {
        PerformanceInsightReport report = builder.build(passingRun(), passedThreshold(), null, null, null, null, List.of(), null);

        assertFalse(report.regressionAvailable());
        assertNull(report.regressionScore());
    }

    @Test
    void mapsAnomalyLevelBoundaries() {
        assertEquals(PerformanceAnomalyLevel.NORMAL, reportWithAnomalySignals(0, 100, 100, 20).anomalyLevel());
        assertEquals(PerformanceAnomalyLevel.WATCH, reportWithAnomalySignals(2, 100, 100, 20).anomalyLevel());
        assertEquals(PerformanceAnomalyLevel.ANOMALOUS, reportWithAnomalySignals(2, 4000, 100, 10).anomalyLevel());
        assertEquals(PerformanceAnomalyLevel.CRITICAL, reportWithAnomalySignals(2, 4000, 11000, 10).anomalyLevel());
    }

    @Test
    void selectsBottleneckType() {
        PerformanceInsightReport latency = builder.build(
                run(0, 20, 100, 4000, 4500, GeneralEnums.PerformanceStatus.COMPLETED),
                passedThreshold(),
                null,
                null,
                null,
                null,
                List.of(),
                null
        );
        PerformanceInsightReport mixed = builder.build(
                run(2, 10, 100, 4000, 4500, GeneralEnums.PerformanceStatus.COMPLETED),
                passedThreshold(),
                null,
                null,
                null,
                null,
                List.of(),
                null
        );

        assertEquals(PerformanceBottleneckType.LATENCY, latency.bottleneckType());
        assertEquals(PerformanceBottleneckType.MIXED, mixed.bottleneckType());
    }

    @Test
    void calculatesReleaseReadiness() {
        PerformanceInsightReport ready = builder.build(passingRun(), passedThreshold(), null, null, availableEnvironment(), null, List.of(), null);
        PerformanceInsightReport blocked = builder.build(run(2, 10, 1200, 4000, 6000, GeneralEnums.PerformanceStatus.COMPLETED), failedThreshold(), null, null, null, null, List.of(), null);
        PerformanceInsightReport unknown = builder.build(run(0, 20, 100, 100, 100, GeneralEnums.PerformanceStatus.RUNNING), passedThreshold(), null, null, null, null, List.of(), null);

        assertEquals(PerformanceReleaseReadiness.READY, ready.releaseReadiness());
        assertEquals(PerformanceReleaseReadiness.BLOCKED, blocked.releaseReadiness());
        assertEquals(PerformanceReleaseReadiness.UNKNOWN, unknown.releaseReadiness());
    }

    @Test
    void createsRootCauseHintsFromEnvironmentMetrics() {
        PerformanceEnvironmentMetrics metrics = new PerformanceEnvironmentMetrics(
                true,
                null,
                70.0,
                90.0,
                60.0,
                70.0,
                90.0,
                1500L,
                48,
                50,
                2,
                1,
                0,
                List.of("warning")
        );

        PerformanceInsightReport report = builder.build(passingRun(), passedThreshold(), null, null, metrics, null, List.of(), null);

        assertEquals(5, report.rootCauseHints().size());
        assertTrue(report.rootCauseHints().stream().anyMatch(hint -> "DATABASE".equals(hint.category())));
        assertTrue(report.rootCauseHints().stream().anyMatch(hint -> "APPLICATION".equals(hint.category())));
    }

    @Test
    void buildsNullSafeReportForLegacyResult() {
        PerformanceInsightReport report = builder.build(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertNotNull(report);
        assertEquals(PerformanceAnomalyLevel.NORMAL, report.anomalyLevel());
        assertEquals(PerformanceReleaseReadiness.UNKNOWN, report.releaseReadiness());
        assertEquals(0.0, report.sloCompliancePercent(), 0.001);
        assertEquals(PerformanceReportVersions.INSIGHT_SCHEMA_VERSION, report.schemaVersion());
        assertEquals(PerformanceReportVersions.INSIGHT_GENERATOR_VERSION, report.generatedByVersion());
    }

    private PerformanceInsightReport reportWithAnomalySignals(double errorRate, double p95, double p99, double throughput) {
        return builder.build(
                run(errorRate, throughput, 100, p95, p99, GeneralEnums.PerformanceStatus.COMPLETED),
                passedThreshold(),
                null,
                null,
                null,
                null,
                List.of(),
                null
        );
    }

    private PerformanceResultItemDto sample(double elapsedTime) {
        PerformanceResultItemDto item = new PerformanceResultItemDto();
        item.setElapsedTime(elapsedTime);
        item.setPerformanceItemStatus(GeneralEnums.PerformanceStatus.COMPLETED);
        return item;
    }

    private PerformanceRunSummary passingRun() {
        return run(0, 25, 500, 900, 1200, GeneralEnums.PerformanceStatus.COMPLETED);
    }

    private PerformanceRunSummary run(
            double errorRate,
            double throughput,
            double average,
            double p95,
            double p99,
            GeneralEnums.PerformanceStatus status
    ) {
        return new PerformanceRunSummary(
                status,
                new Date(0),
                new Date(1000),
                1000,
                1,
                0,
                10,
                Math.round(10 - (10 * errorRate / 100.0)),
                Math.round(10 * errorRate / 100.0),
                errorRate,
                throughput,
                average,
                50,
                p99,
                100,
                500,
                p95,
                p99,
                "step"
        );
    }

    private PerformanceThresholdResult passedThreshold() {
        return new PerformanceThresholdResult(true, "COMPLETED - PASSED", List.of(), PerformanceThresholdConfig.defaults());
    }

    private PerformanceThresholdResult failedThreshold() {
        return new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("failed"), PerformanceThresholdConfig.defaults());
    }

    private PerformanceEnvironmentMetrics availableEnvironment() {
        return new PerformanceEnvironmentMetrics(true, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
    }
}
