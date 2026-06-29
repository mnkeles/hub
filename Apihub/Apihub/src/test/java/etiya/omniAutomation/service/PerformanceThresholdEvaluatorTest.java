package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.common.GeneralEnums;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceThresholdEvaluatorTest {

    @Test
    void evaluatesPassedRunWhenAllThresholdsAreMet() {
        PerformanceThresholdResult result = PerformanceThresholdEvaluator.evaluate(
                runSummary(0.5, 500, 2000, 3000, 25),
                PerformanceThresholdConfig.defaults()
        );

        assertTrue(result.passed());
        assertEquals("COMPLETED - PASSED", result.statusLabel());
        assertTrue(result.reasons().isEmpty());
    }

    @Test
    void evaluatesFailedRunWithAllFailureReasons() {
        PerformanceThresholdResult result = PerformanceThresholdEvaluator.evaluate(
                runSummary(2, 1500, 3500, 7000, 10),
                PerformanceThresholdConfig.defaults()
        );

        assertFalse(result.passed());
        assertEquals("COMPLETED - FAILED", result.statusLabel());
        assertEquals(5, result.reasons().size());
        assertTrue(result.reasons().get(0).contains("Error rate threshold exceeded"));
        assertTrue(result.reasons().get(1).contains("Average response time threshold exceeded"));
        assertTrue(result.reasons().get(2).contains("P95 threshold exceeded"));
        assertTrue(result.reasons().get(3).contains("P99 threshold exceeded"));
        assertTrue(result.reasons().get(4).contains("Throughput threshold not met"));
    }

    @Test
    void mapsThresholdResultToCompletedPassedOrFailedStatus() {
        PerformanceThresholdResult passed = new PerformanceThresholdResult(true, "COMPLETED - PASSED", java.util.List.of(), PerformanceThresholdConfig.defaults());
        PerformanceThresholdResult failed = new PerformanceThresholdResult(false, "COMPLETED - FAILED", java.util.List.of("failed"), PerformanceThresholdConfig.defaults());

        assertEquals(GeneralEnums.PerformanceStatus.COMPLETED_PASSED, PerformanceThresholdEvaluator.finalStatus(passed));
        assertEquals(GeneralEnums.PerformanceStatus.COMPLETED_FAILED, PerformanceThresholdEvaluator.finalStatus(failed));
    }

    private PerformanceRunSummary runSummary(double errorRate, double average, double p95, double p99, double throughput) {
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
                100,
                p99,
                average,
                average,
                p95,
                p99,
                "step"
        );
    }
}
