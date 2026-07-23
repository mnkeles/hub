package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceComparisonMetric;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSloGrade;
import etiya.omniAutomation.business.dto.PerformanceSloScore;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.common.GeneralEnums;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceSloScoreServiceTest {

    private final PerformanceSloScoreService service = new PerformanceSloScoreService();

    @Test
    void gradeAWhenAllMetricsMeetTargets() {
        PerformanceSloScore score = service.calculate(summary(1, 1000, 1500, 500, 50), thresholds(), null);

        assertTrue(score.score() >= 90);
        assertEquals(PerformanceSloGrade.A, score.grade());
    }

    @Test
    void gradeFWhenAllMetricsDoubleTargetsOrWorse() {
        PerformanceSloScore score = service.calculate(summary(10, 6000, 10000, 3000, 5), thresholds(), null);

        assertEquals(PerformanceSloGrade.F, score.grade());
    }

    @Test
    void redistributesBaselineWeightWhenBaselineMissing() {
        PerformanceSloScore score = service.calculate(summary(1, 1000, 1500, 500, 50), thresholds(), null);

        double maxTotal = score.metricScores().stream().mapToDouble(metric -> metric.maxScore()).sum();
        assertEquals(100.0, maxTotal, 0.05);
    }

    @Test
    void baselineRegressionReducesScore() {
        PerformanceComparisonResult comparison = new PerformanceComparisonResult(1L, 2L, List.of(
                new PerformanceComparisonMetric("p95", 100, 200, 100, "UP", false),
                new PerformanceComparisonMetric("p99", 100, 300, 200, "UP", false)
        ));

        PerformanceSloScore score = service.calculate(summary(1, 1000, 1500, 500, 50), thresholds(), comparison);

        assertEquals(0.0, score.metricScores().stream()
                .filter(metric -> "baselineDeviation".equals(metric.metricName()))
                .findFirst()
                .orElseThrow()
                .score());
    }

    @Test
    void nullRunSummaryReturnsNull() {
        assertNull(service.calculate(null, thresholds(), null));
    }

    private PerformanceThresholdConfig thresholds() {
        return new PerformanceThresholdConfig(1, 1000, 3000, 5000, 20);
    }

    private PerformanceRunSummary summary(double errorRate, double p95, double p99, double average, double throughput) {
        return new PerformanceRunSummary(
                GeneralEnums.PerformanceStatus.COMPLETED,
                new Date(0),
                new Date(1000),
                1000,
                1,
                0,
                10,
                10,
                0,
                errorRate,
                throughput,
                average,
                10,
                p99,
                50,
                900,
                p95,
                p99,
                "step"
        );
    }
}
