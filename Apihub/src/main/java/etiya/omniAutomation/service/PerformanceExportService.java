package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAiReport;
import etiya.omniAutomation.business.dto.PerformanceErrorTypeCount;
import etiya.omniAutomation.business.dto.PerformanceExportPayload;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSloMetricScore;
import etiya.omniAutomation.business.dto.PerformanceSloScore;
import etiya.omniAutomation.business.dto.PerformanceStepErrorCount;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.entity.PerfRsltEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.StringJoiner;

@Service
public class PerformanceExportService {

    public PerformanceExportPayload buildPayload(PerfRsltEntity result, PerformanceThreadGroup threadDetail) {
        return new PerformanceExportPayload(
                result.getResultSchemaVersion(),
                result.getThresholdPreset(),
                result.getThresholdConfig(),
                result.getBaseline(),
                result.getBaselineResultId(),
                result.getBaselineComparison(),
                result.getValidationChecklist(),
                result.getRunSummary(),
                result.getThresholdResult(),
                result.getAnalysisSummary(),
                result.getErrorAnalysis(),
                result.getEnvironmentMetrics(),
                result.getAiReport(),
                result.getTestDataId(),
                result.getSloScore(),
                result.getSummary(),
                threadDetail
        );
    }

    public String buildCsv(PerformanceExportPayload payload) {
        StringBuilder csv = new StringBuilder();
        appendReportMetadata(csv, payload);
        appendSloScore(csv, payload == null ? null : payload.sloScore());
        appendAiReport(csv, payload == null ? null : payload.aiReport());
        appendValidationChecklist(csv, payload);
        appendRunSummary(csv, payload == null ? null : payload.runSummary());
        appendThresholdResult(csv, payload == null ? null : payload.thresholdResult());
        appendStepSummary(csv, payload == null ? null : payload.stepSummaries());
        appendErrorSummary(csv, payload);
        return csv.toString();
    }

    private void appendReportMetadata(StringBuilder csv, PerformanceExportPayload payload) {
        csv.append("Report Metadata\n");
        csv.append("Metric,Value\n");
        if (payload != null) {
            row(csv, "Result Schema Version", payload.resultSchemaVersion());
            row(csv, "Threshold Preset", payload.thresholdPreset());
            row(csv, "Baseline", payload.baseline());
            row(csv, "Baseline Result ID", payload.baselineResultId());
            row(csv, "Test Data ID", payload.testDataId());
        }
        csv.append('\n');
    }

    private void appendSloScore(StringBuilder csv, PerformanceSloScore sloScore) {
        csv.append("SLO Score\n");
        csv.append("Field,Value\n");
        if (sloScore != null) {
            row(csv, "Score", sloScore.score());
            row(csv, "Grade", sloScore.grade());
            row(csv, "Status", sloScore.status());
            row(csv, "Strengths", joinList(sloScore.strengths()));
            row(csv, "Weaknesses", joinList(sloScore.weaknesses()));
            row(csv, "Recommendations", joinList(sloScore.recommendations()));
            csv.append("Metric,Score,Max Score,Actual,Target,Direction,Message\n");
            if (sloScore.metricScores() != null) {
                for (PerformanceSloMetricScore metric : sloScore.metricScores()) {
                    row(csv,
                            metric.metricName(),
                            metric.score(),
                            metric.maxScore(),
                            metric.actualValue(),
                            metric.targetValue(),
                            metric.direction(),
                            metric.message());
                }
            }
        }
        csv.append('\n');
    }

    private void appendAiReport(StringBuilder csv, PerformanceAiReport aiReport) {
        csv.append("AI Report\n");
        csv.append("Field,Value\n");
        if (aiReport != null) {
            row(csv, "Overall Status", aiReport.overallStatus());
            row(csv, "Source", aiReport.source());
            row(csv, "Executive Summary", aiReport.executiveSummary());
            row(csv, "Business Impact", aiReport.businessImpact());
            row(csv, "Good Points", joinList(aiReport.goodPoints()));
            row(csv, "Bad Points", joinList(aiReport.badPoints()));
            row(csv, "Risks", joinList(aiReport.risks()));
            row(csv, "Recommended Actions", joinList(aiReport.recommendedActions()));
            row(csv, "Technical Details", aiReport.technicalDetails());
        }
        csv.append('\n');
    }

    private void appendValidationChecklist(StringBuilder csv, PerformanceExportPayload payload) {
        csv.append("Validation Checklist\n");
        csv.append("Key,Status,Message\n");
        if (payload != null && payload.validationChecklist() != null && payload.validationChecklist().items() != null) {
            payload.validationChecklist().items().forEach(item -> row(csv, item.key(), item.status(), item.message()));
        }
        csv.append('\n');
    }

    private void appendRunSummary(StringBuilder csv, PerformanceRunSummary runSummary) {
        csv.append("Run Summary\n");
        csv.append("Metric,Value\n");
        if (runSummary != null) {
            row(csv, "Status", runSummary.status());
            row(csv, "Total Samples", runSummary.totalSamples());
            row(csv, "Successful Samples", runSummary.successfulSamples());
            row(csv, "Failed Samples", runSummary.failedSamples());
            row(csv, "Error Rate", runSummary.errorRate());
            row(csv, "Throughput", runSummary.throughputPerSecond());
            row(csv, "Average", runSummary.averageElapsedTime());
            row(csv, "P90", runSummary.p90ElapsedTime());
            row(csv, "P95", runSummary.p95ElapsedTime());
            row(csv, "P99", runSummary.p99ElapsedTime());
            row(csv, "Slowest Step", runSummary.slowestStepName());
        }
        csv.append('\n');
    }

    private void appendThresholdResult(StringBuilder csv, PerformanceThresholdResult thresholdResult) {
        csv.append("Threshold Result\n");
        csv.append("Passed,Status,Reasons\n");
        if (thresholdResult != null) {
            row(csv, thresholdResult.passed(), thresholdResult.statusLabel(), String.join("; ", thresholdResult.reasons()));
        }
        csv.append('\n');
    }

    private void appendStepSummary(StringBuilder csv, List<PerformanceSummary> summaries) {
        csv.append("Step Summary\n");
        csv.append("Step,Samples,Success,Failure,Error Rate,Throughput,Average,Min,Max,P90,P95,P99,Std Deviation,Last Error\n");
        if (summaries != null) {
            for (PerformanceSummary summary : summaries) {
                row(csv,
                        summary.stepName(),
                        summary.sampleCount(),
                        summary.successCount(),
                        summary.failureCount(),
                        summary.errorRate(),
                        summary.throughputPerSecond(),
                        summary.averageElapsedTime(),
                        summary.minElapsedTime(),
                        summary.maxElapsedTime(),
                        summary.p90ElapsedTime(),
                        summary.p95ElapsedTime(),
                        summary.p99ElapsedTime(),
                        summary.standardDeviation(),
                        summary.lastError());
            }
        }
        csv.append('\n');
    }

    private void appendErrorSummary(StringBuilder csv, PerformanceExportPayload payload) {
        csv.append("Error Summary\n");
        csv.append("Type,Count\n");
        if (payload != null && payload.errorAnalysis() != null && payload.errorAnalysis().errorsByType() != null) {
            for (PerformanceErrorTypeCount errorType : payload.errorAnalysis().errorsByType()) {
                row(csv, errorType.errorType(), errorType.count());
            }
        }
        csv.append("Step,Count\n");
        if (payload != null && payload.errorAnalysis() != null && payload.errorAnalysis().errorsByStep() != null) {
            for (PerformanceStepErrorCount stepError : payload.errorAnalysis().errorsByStep()) {
                row(csv, stepError.stepName(), stepError.count());
            }
        }
    }

    private void row(StringBuilder csv, Object... values) {
        StringJoiner joiner = new StringJoiner(",");
        for (Object value : values) {
            joiner.add(escape(value));
        }
        csv.append(joiner).append('\n');
    }

    private String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        boolean needsQuotes = text.contains(",") || text.contains("\"") || text.contains("\r") || text.contains("\n");
        String escaped = text.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(" | ", values);
    }
}
