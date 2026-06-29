package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAnalysisSummary;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
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

class PerformanceAnalysisBuilderTest {

    @Test
    void selectsProblemStepByThresholdAndErrorScore() {
        PerformanceSummary auth = summary("auth", 100, 1200, 2000, 2500, 0, 0);
        PerformanceSummary createCustomer = summary("createCustomer", 4000, 4500, 6500, 8000, 2, 10);
        PerformanceThresholdResult thresholdResult = new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("failed"), PerformanceThresholdConfig.defaults());

        PerformanceAnalysisSummary analysis = PerformanceAnalysisBuilder.buildAnalysis(
                runSummary(),
                List.of(auth, createCustomer),
                thresholdResult
        );

        assertEquals("createCustomer", analysis.problemStepName());
        assertEquals("createCustomer", analysis.highestP95StepName());
        assertEquals("createCustomer", analysis.highestP99StepName());
        assertEquals("createCustomer", analysis.highestErrorStepName());
    }

    @Test
    void classifiesErrorMessages() {
        assertEquals("HTTP 5xx", PerformanceAnalysisBuilder.classifyError("HTTP 500 internal server error"));
        assertEquals("HTTP 4xx", PerformanceAnalysisBuilder.classifyError("status 404"));
        assertEquals("Timeout", PerformanceAnalysisBuilder.classifyError("Read timed out"));
        assertEquals("Assertion Failed", PerformanceAnalysisBuilder.classifyError("Assertion expected value"));
        assertEquals("Connection Error", PerformanceAnalysisBuilder.classifyError("Connection refused"));
        assertEquals("Database Error", PerformanceAnalysisBuilder.classifyError("SQL exception"));
        assertEquals("Other", PerformanceAnalysisBuilder.classifyError("unknown"));
    }

    @Test
    void groupsErrorsByTypeAndStep() {
        PerformanceThreadGroup group = new PerformanceThreadGroup(List.of(
                new PerformanceThread(0, List.of(
                        failed("auth", "HTTP 500"),
                        failed("customer", "Read timed out")
                )),
                new PerformanceThread(1, List.of(
                        failed("customer", "Read timed out")
                ))
        ));

        PerformanceErrorAnalysis analysis = PerformanceAnalysisBuilder.buildErrorAnalysis(runSummary(), List.of(), group);

        assertEquals(3, analysis.totalErrorCount());
        assertEquals(2, analysis.errorsByType().size());
        assertEquals("HTTP 5xx", analysis.errorsByType().get(0).errorType());
        assertEquals(1, analysis.errorsByType().get(0).count());
        assertEquals("Timeout", analysis.errorsByType().get(1).errorType());
        assertEquals(2, analysis.errorsByType().get(1).count());
        assertEquals(2, analysis.errorsByStep().size());
        assertEquals("Read timed out", analysis.lastError());
    }

    @Test
    void returnsUnavailableEnvironmentMetrics() {
        PerformanceEnvironmentMetrics metrics = PerformanceAnalysisBuilder.unavailableEnvironmentMetrics();

        assertFalse(metrics.metricsAvailable());
        assertEquals("Ortam metrikleri için entegrasyon bulunamadı.", metrics.message());
    }

    private PerformanceSummary summary(String stepName, double average, double max, double p95, double p99, long failures, double errorRate) {
        return new PerformanceSummary(
                stepName,
                max,
                100,
                average,
                10,
                10 - failures,
                failures,
                errorRate,
                5,
                average,
                average,
                p95,
                p99,
                300,
                failures > 0 ? "error" : null,
                new PerformanceResponseTimeBuckets(1, 2, 3, 4)
        );
    }

    private PerformanceRunSummary runSummary() {
        return new PerformanceRunSummary(
                GeneralEnums.PerformanceStatus.COMPLETED,
                new Date(0),
                new Date(1000),
                1000,
                1,
                0,
                10,
                8,
                2,
                20,
                5,
                1000,
                100,
                8000,
                500,
                900,
                6500,
                8000,
                "createCustomer"
        );
    }

    private PerformanceResultItemDto failed(String stepName, String errorMessage) {
        PerformanceResultItemDto item = new PerformanceResultItemDto();
        item.setStepName(stepName);
        item.setThreadNumber(0);
        item.setElapsedTime(1000);
        item.setPerformanceItemStatus(GeneralEnums.PerformanceStatus.FAILED);
        item.setErrorMessage(errorMessage);
        return item;
    }
}
