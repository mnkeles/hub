package etiya.omniAutomation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.PerformanceAiReport;
import etiya.omniAutomation.business.dto.PerformanceAiReportSource;
import etiya.omniAutomation.business.dto.PerformanceAnalysisSummary;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceStepErrorCount;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.common.GeneralEnums;
import etiya.omniAutomation.entity.PerfRsltEntity;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PerformanceAiReportServiceTest {

    @Test
    void parseAiReportResponseReturnsAiSourceWhenJsonParses() throws Exception {
        PerformanceAiReportService service = new PerformanceAiReportService(mock(PerformanceAiClient.class), new ObjectMapper());

        PerformanceAiReport report = service.parseAiReportResponse("""
                {
                  "executiveSummary": "summary",
                  "overallStatus": "FAILED",
                  "businessImpact": "impact",
                  "goodPoints": ["good"],
                  "badPoints": ["bad"],
                  "risks": ["risk"],
                  "recommendedActions": ["action"],
                  "technicalDetails": "details"
                }
                """);

        assertEquals(PerformanceAiReportSource.AI, report.source());
        assertEquals("FAILED", report.overallStatus());
        assertEquals(List.of("good"), report.goodPoints());
        assertNotNull(report.generatedAt());
    }

    @Test
    void generateReportReturnsFallbackWhenAiThrows() {
        PerformanceAiClient aiClient = mock(PerformanceAiClient.class);
        when(aiClient.complete(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new RuntimeException("AI unavailable"));
        PerformanceAiReportService service = new PerformanceAiReportService(aiClient, new ObjectMapper());

        PerformanceAiReport report = service.generateReport(entity(), null);

        assertEquals(PerformanceAiReportSource.FALLBACK, report.source());
        assertFalse(report.recommendedActions().isEmpty());
    }

    @Test
    void generateReportReturnsFallbackWhenAiReturnsIncompletePayload() {
        PerformanceAiClient aiClient = mock(PerformanceAiClient.class);
        when(aiClient.complete(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("{\"executiveSummary\":\"\",\"overallStatus\":\"\",\"businessImpact\":\"\",\"goodPoints\":[],\"badPoints\":[],\"risks\":[],\"recommendedActions\":[],\"technicalDetails\":\"\"}");

        PerformanceAiReportService service = new PerformanceAiReportService(aiClient, new ObjectMapper());

        PerformanceAiReport report = service.generateReport(entity(), null);

        assertEquals(PerformanceAiReportSource.FALLBACK, report.source());
        assertEquals("FAILED", report.overallStatus());
        assertFalse(report.recommendedActions().isEmpty());
    }

    @Test
    void fallbackReportIncludesThresholdFailuresAndActions() {
        PerformanceAiReportService service = new PerformanceAiReportService(mock(PerformanceAiClient.class), new ObjectMapper());

        PerformanceAiReport report = service.fallbackReport(entity(), null, "parse failed");

        assertEquals(PerformanceAiReportSource.FALLBACK, report.source());
        assertFalse(report.badPoints().isEmpty());
        assertFalse(report.recommendedActions().isEmpty());
        assertFalse(report.risks().isEmpty());
        assertEquals("FAILED", report.overallStatus());
        assertTrue(report.executiveSummary().contains("Performans testi"));
    }

    @Test
    void generateReportReturnsAiSourceWhenClientReturnsStructuredJson() {
        PerformanceAiClient aiClient = mock(PerformanceAiClient.class);
        when(aiClient.complete(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("""
                        {
                          "executiveSummary": "summary",
                          "overallStatus": "FAILED",
                          "businessImpact": "impact",
                          "goodPoints": ["good"],
                          "badPoints": ["bad"],
                          "risks": ["risk"],
                          "recommendedActions": ["action"],
                          "technicalDetails": "details"
                        }
                        """);

        PerformanceAiReportService service = new PerformanceAiReportService(aiClient, new ObjectMapper());

        PerformanceAiReport report = service.generateReport(entity(), null);

        assertEquals(PerformanceAiReportSource.AI, report.source());
        assertEquals("summary", report.executiveSummary());
        assertEquals(List.of("action"), report.recommendedActions());
    }

    private PerfRsltEntity entity() {
        PerfRsltEntity entity = new PerfRsltEntity();
        entity.setPerfStatus(GeneralEnums.PerformanceStatus.COMPLETED_FAILED);
        entity.setRunSummary(new PerformanceRunSummary(GeneralEnums.PerformanceStatus.COMPLETED, new Date(0), new Date(1000), 1000,
                10, 0, 100, 96, 4, 4, 12, 1800, 200, 9000, 900, 1200, 6000, 8500, "createCustomer"));
        entity.setThresholdResult(new PerformanceThresholdResult(false, "COMPLETED - FAILED",
                List.of("P95 threshold exceeded. Expected <= 3000 ms, actual: 6000 ms"),
                PerformanceThresholdConfig.defaults()));
        entity.setAnalysisSummary(new PerformanceAnalysisSummary(
                GeneralEnums.PerformanceStatus.COMPLETED_FAILED,
                entity.getThresholdResult(),
                "createCustomer",
                "createCustomer",
                "createCustomer",
                "createCustomer",
                "createCustomer",
                "createCustomer",
                "Problem step createCustomer",
                List.of()
        ));
        entity.setErrorAnalysis(new PerformanceErrorAnalysis(4, 4,
                List.of(),
                List.of(new PerformanceStepErrorCount("createCustomer", 4)),
                "HTTP 500",
                List.of()));
        entity.setEnvironmentMetrics(new PerformanceEnvironmentMetrics(false, "unavailable", null, null, null, null, null, null, null, null, null, null, null, List.of()));
        entity.setSummary(List.of(new PerformanceSummary("createCustomer", 9000, 200, 1800, 100, 96, 4, 4, 12, 900, 1200, 6000, 8500, 700, "HTTP 500", null)));
        return entity;
    }
}
