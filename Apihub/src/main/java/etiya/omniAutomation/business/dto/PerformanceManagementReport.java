package etiya.omniAutomation.business.dto;

import java.util.List;

public record PerformanceManagementReport(
        String overallStatus,
        PerformanceManagementRiskLevel riskLevel,
        String stepAssessmentSummary,
        List<PerformanceManagementStepAssessment> stepAssessments,
        String executiveSummary,
        List<PerformanceManagementSlaItem> slaSummary,
        List<PerformanceManagementProblemArea> problemAreas,
        String trendSummary,
        List<String> recommendedActions,
        String detailExplanation
) {
}
