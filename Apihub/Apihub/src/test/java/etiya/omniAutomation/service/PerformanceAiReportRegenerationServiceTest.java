package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAiManagementReport;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.common.GeneralEnums;
import etiya.omniAutomation.entity.PerfRsltEntity;
import etiya.omniAutomation.repository.PerformanceResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PerformanceAiReportRegenerationServiceTest {

    @Test
    void regeneratesAndOverwritesAiReport() {
        PerformanceResultRepository repository = mock(PerformanceResultRepository.class);
        PerformanceAiReportService aiReportService = mock(PerformanceAiReportService.class);
        PerformanceAiManagementReport generatedReport = generatedReport();
        PerfRsltEntity entity = entity();
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(aiReportService.generate(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(generatedReport);

        PerformanceAiManagementReport result = service(repository, aiReportService).regenerate(1L, new PerformanceThreadGroup(List.of()));

        assertSame(generatedReport, result);
        assertSame(generatedReport, entity.getAiManagementReport());
        verify(repository).save(entity);
    }

    @Test
    void persistsFallbackWithoutChangingRunStatus() {
        PerformanceResultRepository repository = mock(PerformanceResultRepository.class);
        PerformanceAiReportService aiReportService = mock(PerformanceAiReportService.class);
        PerfRsltEntity entity = entity();
        entity.setPerfStatus(GeneralEnums.PerformanceStatus.COMPLETED_FAILED);
        PerformanceAiManagementReport fallback = PerformanceAiManagementReport.notGenerated("not generated");
        when(repository.findById(1L)).thenReturn(Optional.of(entity));
        when(aiReportService.generate(any(), any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(fallback);

        PerformanceAiManagementReport result = service(repository, aiReportService).regenerate(1L, null);

        assertFalse(result.generated());
        assertEquals(GeneralEnums.PerformanceStatus.COMPLETED_FAILED, entity.getPerfStatus());
        assertSame(fallback, entity.getAiManagementReport());
        verify(repository).save(entity);
    }

    @Test
    void throwsNotFoundForMissingResult() {
        PerformanceResultRepository repository = mock(PerformanceResultRepository.class);
        when(repository.findById(404L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service(repository, mock(PerformanceAiReportService.class)).regenerate(404L, null)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(repository).findById(eq(404L));
    }

    private PerformanceAiReportRegenerationService service(
            PerformanceResultRepository repository,
            PerformanceAiReportService aiReportService
    ) {
        return new PerformanceAiReportRegenerationService(
                repository,
                new PerformanceManagementReportBuilder(),
                new PerformanceInsightBuilder(),
                aiReportService
        );
    }

    private PerfRsltEntity entity() {
        PerfRsltEntity entity = new PerfRsltEntity();
        entity.setPerfRsltId(1L);
        entity.setPerfStatus(GeneralEnums.PerformanceStatus.COMPLETED);
        entity.setRunSummary(new PerformanceRunSummary(
                GeneralEnums.PerformanceStatus.COMPLETED,
                new Date(0),
                new Date(1000),
                1000,
                1,
                0,
                10,
                9,
                1,
                10,
                20,
                100,
                10,
                500,
                50,
                90,
                95,
                99,
                "step"
        ));
        entity.setThresholdResult(new PerformanceThresholdResult(false, "COMPLETED - FAILED", List.of("reason"), PerformanceThresholdConfig.defaults()));
        entity.setSummary(List.of());
        return entity;
    }

    private PerformanceAiManagementReport generatedReport() {
        return new PerformanceAiManagementReport(
                true,
                new Date(),
                "fake-model",
                "Executive",
                "Technical",
                "Root cause",
                List.of(),
                "Readiness",
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
}
