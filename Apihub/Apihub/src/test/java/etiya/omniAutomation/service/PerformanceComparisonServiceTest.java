package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.common.GeneralEnums;
import etiya.omniAutomation.entity.PerfRsltEntity;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceComparisonServiceTest {

    private final PerformanceComparisonService service = new PerformanceComparisonService();

    @Test
    void marksLatencyIncreaseAsRegression() {
        PerformanceComparisonResult result = service.compare(entity(1L, summary(100, 90, 95, 99, 20, 0, 10, "a")),
                entity(2L, summary(200, 180, 195, 250, 20, 0, 10, "a")));

        assertEquals("averageResponseTime", result.metrics().get(0).metricName());
        assertEquals("REGRESSED", result.metrics().get(0).direction());
        assertFalse(result.metrics().get(0).improvement());
    }

    @Test
    void marksThroughputIncreaseAsImprovement() {
        PerformanceComparisonResult result = service.compare(entity(1L, summary(100, 90, 95, 99, 10, 0, 10, "a")),
                entity(2L, summary(100, 90, 95, 99, 30, 0, 10, "a")));

        assertEquals("throughput", result.metrics().get(4).metricName());
        assertEquals("IMPROVED", result.metrics().get(4).direction());
        assertTrue(result.metrics().get(4).improvement());
    }

    @Test
    void treatsTotalSamplesAsInformational() {
        PerformanceComparisonResult result = service.compare(entity(1L, summary(100, 90, 95, 99, 10, 0, 10, "a")),
                entity(2L, summary(100, 90, 95, 99, 10, 0, 20, "a")));

        assertEquals("totalSamples", result.metrics().get(6).metricName());
        assertEquals("CHANGED", result.metrics().get(6).direction());
        assertNull(result.metrics().get(6).improvement());
    }

    private PerfRsltEntity entity(Long id, PerformanceRunSummary summary) {
        PerfRsltEntity entity = new PerfRsltEntity();
        entity.setPerfRsltId(id);
        entity.setRunSummary(summary);
        return entity;
    }

    private PerformanceRunSummary summary(double average, double p90, double p95, double p99, double throughput, double errorRate, long totalSamples, String slowestStep) {
        return new PerformanceRunSummary(GeneralEnums.PerformanceStatus.COMPLETED, new Date(0), new Date(1000), 1000,
                1, 0, totalSamples, totalSamples, 0, errorRate, throughput, average, 1, p99, average, p90, p95, p99, slowestStep);
    }
}
