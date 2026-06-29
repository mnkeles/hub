package etiya.omniAutomation.business.dto;

import java.util.List;

public record PerformanceExportPayload(
        Integer resultSchemaVersion,
        PerformanceThresholdPreset thresholdPreset,
        PerformanceThresholdConfig thresholdConfig,
        Boolean baseline,
        Long baselineResultId,
        PerformanceComparisonResult baselineComparison,
        PerformanceValidationChecklist validationChecklist,
        PerformanceRunSummary runSummary,
        PerformanceThresholdResult thresholdResult,
        PerformanceAnalysisSummary analysisSummary,
        PerformanceErrorAnalysis errorAnalysis,
        PerformanceEnvironmentMetrics environmentMetrics,
        PerformanceManagementReport managementReport,
        List<PerformanceSummary> stepSummaries,
        PerformanceThreadGroup threadDetail
) {
}
