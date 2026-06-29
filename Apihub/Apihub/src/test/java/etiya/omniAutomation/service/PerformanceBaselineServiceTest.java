package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.entity.PerfRsltEntity;
import etiya.omniAutomation.repository.PerformanceResultRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class PerformanceBaselineServiceTest {

    @Test
    void setBaselineClearsPreviousBaselineForSameFlow() {
        PerformanceResultRepository repository = mock(PerformanceResultRepository.class);
        PerformanceBaselineService service = new PerformanceBaselineService(repository, mock(PerformanceComparisonService.class));
        PerfRsltEntity oldBaseline = result(1L, 10L, 20L, true);
        PerfRsltEntity target = result(2L, 10L, 20L, false);
        when(repository.findById(2L)).thenReturn(Optional.of(target));
        when(repository.findByProjectIdAndProcessFlowIdAndBaselineTrue(10L, 20L)).thenReturn(List.of(oldBaseline));
        when(repository.save(target)).thenReturn(target);

        PerfRsltEntity updated = service.setBaseline(2L);

        assertFalse(oldBaseline.getBaseline());
        assertTrue(updated.getBaseline());
        verify(repository).saveAll(List.of(oldBaseline));
        verify(repository).save(target);
    }

    @Test
    void setBaselineDoesNotClearDifferentFlowBaseline() {
        PerformanceResultRepository repository = mock(PerformanceResultRepository.class);
        PerformanceBaselineService service = new PerformanceBaselineService(repository, mock(PerformanceComparisonService.class));
        PerfRsltEntity target = result(2L, 10L, 20L, false);
        when(repository.findById(2L)).thenReturn(Optional.of(target));
        when(repository.findByProjectIdAndProcessFlowIdAndBaselineTrue(10L, 20L)).thenReturn(List.of());
        when(repository.save(target)).thenReturn(target);

        service.setBaseline(2L);

        verify(repository).findByProjectIdAndProcessFlowIdAndBaselineTrue(10L, 20L);
        assertTrue(target.getBaseline());
    }

    @Test
    void applyAutomaticBaselineComparisonStoresBaselineResultIdAndComparison() {
        PerformanceResultRepository repository = mock(PerformanceResultRepository.class);
        PerformanceComparisonService comparisonService = mock(PerformanceComparisonService.class);
        PerformanceBaselineService service = new PerformanceBaselineService(repository, comparisonService);
        PerfRsltEntity baseline = result(1L, 10L, 20L, true);
        PerfRsltEntity completed = result(2L, 10L, 20L, false);
        PerformanceComparisonResult comparison = new PerformanceComparisonResult(1L, 2L, List.of());
        when(repository.findFirstByProjectIdAndProcessFlowIdAndBaselineTrue(10L, 20L)).thenReturn(Optional.of(baseline));
        when(comparisonService.compare(baseline, completed)).thenReturn(comparison);
        when(repository.save(completed)).thenReturn(completed);

        PerfRsltEntity updated = service.applyAutomaticBaselineComparison(completed);

        assertEquals(1L, updated.getBaselineResultId());
        assertSame(comparison, updated.getBaselineComparison());
        verify(repository).save(completed);
    }

    @Test
    void applyAutomaticBaselineComparisonSkipsWhenNoBaselineExists() {
        PerformanceResultRepository repository = mock(PerformanceResultRepository.class);
        PerformanceComparisonService comparisonService = mock(PerformanceComparisonService.class);
        PerformanceBaselineService service = new PerformanceBaselineService(repository, comparisonService);
        PerfRsltEntity completed = result(2L, 10L, 20L, false);
        when(repository.findFirstByProjectIdAndProcessFlowIdAndBaselineTrue(10L, 20L)).thenReturn(Optional.empty());

        PerfRsltEntity updated = service.applyAutomaticBaselineComparison(completed);

        assertSame(completed, updated);
        assertNull(updated.getBaselineResultId());
        assertNull(updated.getBaselineComparison());
        verify(comparisonService, never()).compare(any(PerfRsltEntity.class), any(PerfRsltEntity.class));
    }

    @Test
    void applyAutomaticBaselineComparisonSkipsWhenCompletedRunIsBaseline() {
        PerformanceResultRepository repository = mock(PerformanceResultRepository.class);
        PerformanceComparisonService comparisonService = mock(PerformanceComparisonService.class);
        PerformanceBaselineService service = new PerformanceBaselineService(repository, comparisonService);
        PerfRsltEntity completed = result(2L, 10L, 20L, true);
        when(repository.findFirstByProjectIdAndProcessFlowIdAndBaselineTrue(10L, 20L)).thenReturn(Optional.of(completed));

        PerfRsltEntity updated = service.applyAutomaticBaselineComparison(completed);

        assertSame(completed, updated);
        assertNull(updated.getBaselineResultId());
        assertNull(updated.getBaselineComparison());
        verify(comparisonService, never()).compare(any(PerfRsltEntity.class), any(PerfRsltEntity.class));
    }

    private PerfRsltEntity result(Long id, Long projectId, Long processFlowId, boolean baseline) {
        PerfRsltEntity entity = new PerfRsltEntity();
        entity.setPerfRsltId(id);
        entity.setProjectId(projectId);
        entity.setProcessFlowId(processFlowId);
        entity.setBaseline(baseline);
        return entity;
    }
}
