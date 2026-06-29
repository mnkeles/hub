package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceComparisonMetric;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceValidationChecklist;
import etiya.omniAutomation.business.dto.PerformanceValidationChecklistItem;
import etiya.omniAutomation.business.dto.PerformanceValidationStatus;
import etiya.omniAutomation.common.GeneralEnums;
import etiya.omniAutomation.entity.PerfRsltEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class PerformanceValidationChecklistBuilder {

    public PerformanceValidationChecklist build(PerfRsltEntity result, PerformanceThreadGroup threadGroup) {
        List<PerformanceValidationChecklistItem> items = new ArrayList<>();
        items.add(runCompleted(result));
        items.add(thresholdEvaluated(result));
        items.add(baselineComparison(result));
        items.add(baselinePrimaryMetrics(result));
        items.add(errorAnalysis(result));
        items.add(exportReady(result, threadGroup));
        items.add(liveStopState(result));
        items.add(environmentMetrics(result));

        PerformanceValidationChecklist current = result == null ? null : result.getValidationChecklist();
        return new PerformanceValidationChecklist(
                items,
                current == null ? null : current.manualNote(),
                current == null ? null : current.manualNoteUpdatedAt()
        );
    }

    public PerformanceValidationChecklist withManualNote(PerformanceValidationChecklist current, String note, Date updatedAt) {
        return new PerformanceValidationChecklist(
                current == null ? List.of() : current.items(),
                note,
                updatedAt
        );
    }

    private PerformanceValidationChecklistItem runCompleted(PerfRsltEntity result) {
        GeneralEnums.PerformanceStatus status = result == null ? null : result.getPerfStatus();
        if (status == GeneralEnums.PerformanceStatus.COMPLETED_PASSED || status == GeneralEnums.PerformanceStatus.COMPLETED_FAILED) {
            return item("run_completed", "Run technically completed", PerformanceValidationStatus.PASSED, "Run reached a completed terminal status.");
        }
        if (status == GeneralEnums.PerformanceStatus.STOPPED) {
            return item("run_completed", "Run technically completed", PerformanceValidationStatus.WARNING, "Run was stopped before normal completion.");
        }
        if (status == GeneralEnums.PerformanceStatus.ERROR) {
            return item("run_completed", "Run technically completed", PerformanceValidationStatus.FAILED, "Run ended with an execution error.");
        }
        return item("run_completed", "Run technically completed", PerformanceValidationStatus.NOT_APPLICABLE, "Run is not in a terminal status yet.");
    }

    private PerformanceValidationChecklistItem thresholdEvaluated(PerfRsltEntity result) {
        if (result == null || result.getThresholdResult() == null) {
            return item("threshold_evaluated", "Threshold evaluation", PerformanceValidationStatus.NOT_APPLICABLE, "Threshold result is not available.");
        }
        if (result.getThresholdResult().passed()) {
            return item("threshold_evaluated", "Threshold evaluation", PerformanceValidationStatus.PASSED, "All effective thresholds passed.");
        }
        return item("threshold_evaluated", "Threshold evaluation", PerformanceValidationStatus.FAILED, String.join("; ", result.getThresholdResult().reasons()));
    }

    private PerformanceValidationChecklistItem baselineComparison(PerfRsltEntity result) {
        if (result == null || result.getBaselineResultId() == null) {
            return item("baseline_comparison", "Baseline comparison", PerformanceValidationStatus.NOT_APPLICABLE, "No baseline was available for this run.");
        }
        if (result.getBaselineComparison() != null) {
            return item("baseline_comparison", "Baseline comparison", PerformanceValidationStatus.PASSED, "Baseline comparison was generated.");
        }
        return item("baseline_comparison", "Baseline comparison", PerformanceValidationStatus.WARNING, "Baseline was selected but comparison data is missing.");
    }

    private PerformanceValidationChecklistItem baselinePrimaryMetrics(PerfRsltEntity result) {
        PerformanceComparisonResult comparison = result == null ? null : result.getBaselineComparison();
        if (comparison == null || comparison.metrics() == null || comparison.metrics().isEmpty()) {
            return item("baseline_primary_metrics", "Baseline primary metrics", PerformanceValidationStatus.NOT_APPLICABLE, "No baseline metrics are available.");
        }
        boolean regressed = comparison.metrics().stream()
                .map(PerformanceComparisonMetric::improvement)
                .anyMatch(Boolean.FALSE::equals);
        if (regressed) {
            return item("baseline_primary_metrics", "Baseline primary metrics", PerformanceValidationStatus.FAILED, "At least one primary metric regressed against baseline.");
        }
        return item("baseline_primary_metrics", "Baseline primary metrics", PerformanceValidationStatus.PASSED, "No primary metric regressed against baseline.");
    }

    private PerformanceValidationChecklistItem errorAnalysis(PerfRsltEntity result) {
        if (result != null && result.getErrorAnalysis() != null) {
            return item("error_analysis", "Error analysis", PerformanceValidationStatus.PASSED, "Error analysis was generated.");
        }
        return item("error_analysis", "Error analysis", PerformanceValidationStatus.WARNING, "Error analysis is not available.");
    }

    private PerformanceValidationChecklistItem exportReady(PerfRsltEntity result, PerformanceThreadGroup threadGroup) {
        if (result == null || result.getRunSummary() == null) {
            return item("export_ready", "Export data", isTerminal(result) ? PerformanceValidationStatus.FAILED : PerformanceValidationStatus.NOT_APPLICABLE, "Run summary is not available.");
        }
        if (result.getSummary() != null || threadGroup != null) {
            return item("export_ready", "Export data", PerformanceValidationStatus.PASSED, "Export payload can be generated.");
        }
        return item("export_ready", "Export data", PerformanceValidationStatus.WARNING, "Run summary exists but detail data is missing.");
    }

    private PerformanceValidationChecklistItem liveStopState(PerfRsltEntity result) {
        if (isTerminal(result)) {
            return item("live_stop_state", "Live/stop state", PerformanceValidationStatus.PASSED, "Run is terminal; live stop controls are no longer needed.");
        }
        return item("live_stop_state", "Live/stop state", PerformanceValidationStatus.WARNING, "Run is still active or stopping.");
    }

    private PerformanceValidationChecklistItem environmentMetrics(PerfRsltEntity result) {
        PerformanceEnvironmentMetrics metrics = result == null ? null : result.getEnvironmentMetrics();
        if (metrics == null) {
            return item("environment_metrics", "Environment metrics", PerformanceValidationStatus.NOT_APPLICABLE, "Environment metrics were not collected.");
        }
        if (metrics.metricsAvailable()) {
            return item("environment_metrics", "Environment metrics", PerformanceValidationStatus.PASSED, "Environment metrics are available.");
        }
        if (metrics.message() != null && !metrics.message().isBlank()) {
            return item("environment_metrics", "Environment metrics", PerformanceValidationStatus.WARNING, metrics.message());
        }
        return item("environment_metrics", "Environment metrics", PerformanceValidationStatus.WARNING, "Environment metrics are unavailable.");
    }

    private boolean isTerminal(PerfRsltEntity result) {
        if (result == null) {
            return false;
        }
        GeneralEnums.PerformanceStatus status = result.getPerfStatus();
        return status == GeneralEnums.PerformanceStatus.COMPLETED_PASSED
                || status == GeneralEnums.PerformanceStatus.COMPLETED_FAILED
                || status == GeneralEnums.PerformanceStatus.STOPPED
                || status == GeneralEnums.PerformanceStatus.ERROR;
    }

    private PerformanceValidationChecklistItem item(String key, String label, PerformanceValidationStatus status, String message) {
        return new PerformanceValidationChecklistItem(key, label, status, message);
    }
}
