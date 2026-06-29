package etiya.omniAutomation.results;

import etiya.omniAutomation.business.dto.PerformanceAnalysisSummary;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdPreset;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.business.dto.PerformanceValidationChecklist;
import etiya.omniAutomation.common.GeneralEnums;

import java.util.Date;
import java.util.List;

public record PerformanceSummaryResult(
        Long performanceResultId,
        GeneralEnums.PerformanceStatus performanceStatus,
        Integer threadCount,
        Integer rampUpPeriod,
        Integer durationSeconds,
        Integer loopCount,
        Integer thinkTimeMs,
        Integer timeoutMs,
        String environmentBaseUrl,
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
        List<PerformanceSummary> performanceSummaries,
        Date createdAt
) {

    public PerformanceSummaryResult(
            Long performanceResultId,
            GeneralEnums.PerformanceStatus performanceStatus,
            Integer threadCount,
            Integer rampUpPeriod,
            PerformanceRunSummary runSummary,
            List<PerformanceSummary> performanceSummaries,
            Date createdAt
    ) {
        this(performanceResultId, performanceStatus, threadCount, rampUpPeriod, null, null, null, null, null,
                null, null, null, null, null, null, null,
                runSummary, null, null, null, null, performanceSummaries, createdAt);
    }
}
