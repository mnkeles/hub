package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceComparisonMetric;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.business.dto.PerformanceValidationChecklist;
import etiya.omniAutomation.business.dto.PerformanceValidationStatus;
import etiya.omniAutomation.common.GeneralEnums;
import etiya.omniAutomation.entity.PerfRsltEntity;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PerformanceValidationChecklistBuilderTest {

    private final PerformanceValidationChecklistBuilder builder = new PerformanceValidationChecklistBuilder();

    @Test
    void completedPassedRunReturnsPassedRunAndThresholdItems() {
        PerfRsltEntity result = terminal(GeneralEnums.PerformanceStatus.COMPLETED_PASSED);
        result.setThresholdResult(new PerformanceThresholdResult(true, "PASSED", List.of(), PerformanceThresholdConfig.defaults()));

        PerformanceValidationChecklist checklist = builder.build(result, new PerformanceThreadGroup(List.of()));

        assertEquals(PerformanceValidationStatus.PASSED, status(checklist, "run_completed"));
        assertEquals(PerformanceValidationStatus.PASSED, status(checklist, "threshold_evaluated"));
    }

    @Test
    void completedFailedRunReturnsFailedThresholdItem() {
        PerfRsltEntity result = terminal(GeneralEnums.PerformanceStatus.COMPLETED_FAILED);
        result.setThresholdResult(new PerformanceThresholdResult(false, "FAILED", List.of("p95"), PerformanceThresholdConfig.defaults()));

        PerformanceValidationChecklist checklist = builder.build(result, new PerformanceThreadGroup(List.of()));

        assertEquals(PerformanceValidationStatus.FAILED, status(checklist, "threshold_evaluated"));
    }

    @Test
    void missingBaselineReturnsNotApplicableBaselineItems() {
        PerfRsltEntity result = terminal(GeneralEnums.PerformanceStatus.COMPLETED_PASSED);

        PerformanceValidationChecklist checklist = builder.build(result, new PerformanceThreadGroup(List.of()));

        assertEquals(PerformanceValidationStatus.NOT_APPLICABLE, status(checklist, "baseline_comparison"));
        assertEquals(PerformanceValidationStatus.NOT_APPLICABLE, status(checklist, "baseline_primary_metrics"));
    }

    @Test
    void baselineRegressionReturnsFailedPrimaryMetricItem() {
        PerfRsltEntity result = terminal(GeneralEnums.PerformanceStatus.COMPLETED_PASSED);
        result.setBaselineResultId(1L);
        result.setBaselineComparison(new PerformanceComparisonResult(1L, 2L, List.of(
                new PerformanceComparisonMetric("p95", 100, 200, 100, "REGRESSED", false)
        )));

        PerformanceValidationChecklist checklist = builder.build(result, new PerformanceThreadGroup(List.of()));

        assertEquals(PerformanceValidationStatus.FAILED, status(checklist, "baseline_primary_metrics"));
    }

    @Test
    void unavailableEnvironmentMetricsReturnsWarning() {
        PerfRsltEntity result = terminal(GeneralEnums.PerformanceStatus.COMPLETED_PASSED);
        result.setEnvironmentMetrics(new PerformanceEnvironmentMetrics(false, "Metrics unavailable", null, null, null, null, null, null, null, null, null, null, null, List.of()));

        PerformanceValidationChecklist checklist = builder.build(result, new PerformanceThreadGroup(List.of()));

        assertEquals(PerformanceValidationStatus.WARNING, status(checklist, "environment_metrics"));
    }

    @Test
    void manualNoteUpdatePreservesItemCountAndSetsTimestamp() {
        PerformanceValidationChecklist current = builder.build(terminal(GeneralEnums.PerformanceStatus.COMPLETED_PASSED), new PerformanceThreadGroup(List.of()));
        Date updatedAt = new Date();

        PerformanceValidationChecklist updated = builder.withManualNote(current, "checked manually", updatedAt);

        assertEquals(current.items().size(), updated.items().size());
        assertEquals("checked manually", updated.manualNote());
        assertEquals(updatedAt, updated.manualNoteUpdatedAt());
    }

    private PerformanceValidationStatus status(PerformanceValidationChecklist checklist, String key) {
        return checklist.items().stream()
                .filter(item -> item.key().equals(key))
                .findFirst()
                .map(item -> {
                    assertNotNull(item.message());
                    return item.status();
                })
                .orElseThrow();
    }

    private PerfRsltEntity terminal(GeneralEnums.PerformanceStatus status) {
        PerfRsltEntity result = new PerfRsltEntity();
        result.setPerfStatus(status);
        result.setRunSummary(new PerformanceRunSummary(status, new Date(0), new Date(1), 1, 1, 0, 1, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, "step"));
        return result;
    }
}
