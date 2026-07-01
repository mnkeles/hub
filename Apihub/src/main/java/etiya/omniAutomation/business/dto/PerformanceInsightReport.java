package etiya.omniAutomation.business.dto;

import java.util.List;

public record PerformanceInsightReport(
        double anomalyScore,
        PerformanceAnomalyLevel anomalyLevel,
        Double regressionScore,
        boolean regressionAvailable,
        Double apdexScore,
        boolean apdexEstimated,
        double sloCompliancePercent,
        PerformanceBottleneckType bottleneckType,
        PerformanceReleaseReadiness releaseReadiness,
        List<PerformanceRootCauseHint> rootCauseHints,
        List<PerformanceMetricInsight> metricInsights,
        List<PerformanceStepInsight> stepInsights,
        Integer schemaVersion,
        String generatedByVersion
) {
}
