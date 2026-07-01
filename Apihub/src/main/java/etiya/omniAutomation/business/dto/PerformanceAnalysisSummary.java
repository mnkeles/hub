package etiya.omniAutomation.business.dto;

import etiya.omniAutomation.common.GeneralEnums;

import java.util.List;

public record PerformanceAnalysisSummary(
        GeneralEnums.PerformanceStatus status,
        PerformanceThresholdResult thresholdResult,
        String problemStepName,
        String slowestStepName,
        String highestP95StepName,
        String highestP99StepName,
        String highestErrorStepName,
        String highestStdDeviationStepName,
        String summaryText,
        List<String> warnings
) {
}
