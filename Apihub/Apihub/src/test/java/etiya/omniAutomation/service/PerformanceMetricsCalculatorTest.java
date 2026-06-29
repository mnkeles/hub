package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceResponseTimeBuckets;
import etiya.omniAutomation.business.dto.PerformanceResultItemDto;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.common.GeneralEnums;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PerformanceMetricsCalculatorTest {

    @Test
    void calculatesPercentilesWithCeilingIndex() {
        PerformanceRunSummary summary = PerformanceMetricsCalculator.buildRunSummary(
                List.of(
                        sample(100, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(200, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(300, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(400, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(500, GeneralEnums.PerformanceStatus.COMPLETED, "A")
                ),
                new Date(0),
                new Date(1000),
                1,
                0,
                GeneralEnums.PerformanceStatus.COMPLETED
        );

        assertEquals(300, summary.p50ElapsedTime());
        assertEquals(500, summary.p90ElapsedTime());
        assertEquals(500, summary.p95ElapsedTime());
        assertEquals(500, summary.p99ElapsedTime());
    }

    @Test
    void calculatesPopulationStandardDeviation() {
        PerformanceSummary summary = PerformanceMetricsCalculator.buildStepSummaries(
                Map.of(1L, "A"),
                Map.of(1L, List.of(
                        sample(100, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(200, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(300, GeneralEnums.PerformanceStatus.COMPLETED, "A")
                )),
                1000
        ).get(0);

        assertEquals(81.65, summary.standardDeviation(), 0.01);
    }

    @Test
    void calculatesErrorRate() {
        PerformanceRunSummary summary = PerformanceMetricsCalculator.buildRunSummary(
                List.of(
                        sample(100, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(200, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(300, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(400, GeneralEnums.PerformanceStatus.FAILED, "A")
                ),
                new Date(0),
                new Date(1000),
                1,
                0,
                GeneralEnums.PerformanceStatus.COMPLETED
        );

        assertEquals(25.0, summary.errorRate());
    }

    @Test
    void calculatesThroughputPerSecond() {
        PerformanceRunSummary summary = PerformanceMetricsCalculator.buildRunSummary(
                List.of(
                        sample(100, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(200, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(300, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(400, GeneralEnums.PerformanceStatus.COMPLETED, "A")
                ),
                new Date(0),
                new Date(2000),
                1,
                0,
                GeneralEnums.PerformanceStatus.COMPLETED
        );

        assertEquals(2.0, summary.throughputPerSecond());
    }

    @Test
    void calculatesResponseTimeBuckets() {
        PerformanceSummary summary = PerformanceMetricsCalculator.buildStepSummaries(
                Map.of(1L, "A"),
                Map.of(1L, List.of(
                        sample(100, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(750, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(1500, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(3500, GeneralEnums.PerformanceStatus.COMPLETED, "A")
                )),
                1000
        ).get(0);
        PerformanceResponseTimeBuckets buckets = summary.responseTimeBuckets();

        assertEquals(1, buckets.under500ms());
        assertEquals(1, buckets.from500msTo1s());
        assertEquals(1, buckets.from1sTo3s());
        assertEquals(1, buckets.over3s());
    }

    @Test
    void returnsZeroLatencyMetricsWhenThereAreNoMeasuredElapsedTimes() {
        PerformanceRunSummary summary = PerformanceMetricsCalculator.buildRunSummary(
                List.of(
                        sample(0, GeneralEnums.PerformanceStatus.COMPLETED, "A"),
                        sample(-1, GeneralEnums.PerformanceStatus.FAILED, "A")
                ),
                new Date(0),
                new Date(0),
                1,
                0,
                GeneralEnums.PerformanceStatus.FAILED
        );

        assertEquals(0, summary.averageElapsedTime());
        assertEquals(0, summary.minElapsedTime());
        assertEquals(0, summary.maxElapsedTime());
        assertEquals(0, summary.p50ElapsedTime());
        assertEquals(0, summary.p90ElapsedTime());
        assertEquals(0, summary.throughputPerSecond());
    }

    private PerformanceResultItemDto sample(double elapsedTime, GeneralEnums.PerformanceStatus status, String stepName) {
        PerformanceResultItemDto sample = new PerformanceResultItemDto();
        sample.setProcessFlowStepId(1L);
        sample.setStepName(stepName);
        sample.setElapsedTime(elapsedTime);
        sample.setPerformanceItemStatus(status);
        return sample;
    }
}
