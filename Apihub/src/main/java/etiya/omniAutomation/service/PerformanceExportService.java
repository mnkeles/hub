package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAiActionItem;
import etiya.omniAutomation.business.dto.PerformanceErrorTypeCount;
import etiya.omniAutomation.business.dto.PerformanceExportPayload;
import etiya.omniAutomation.business.dto.PerformanceAiManagementReport;
import etiya.omniAutomation.business.dto.PerformanceInsightReport;
import etiya.omniAutomation.business.dto.PerformanceManagementReport;
import etiya.omniAutomation.business.dto.PerformanceMetricInsight;
import etiya.omniAutomation.business.dto.PerformanceRootCauseHint;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
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

    private final PerformanceManagementReportBuilder performanceManagementReportBuilder;
    private final PerformanceInsightBuilder performanceInsightBuilder;

    public PerformanceExportService(
            PerformanceManagementReportBuilder performanceManagementReportBuilder,
            PerformanceInsightBuilder performanceInsightBuilder
    ) {
        this.performanceManagementReportBuilder = performanceManagementReportBuilder;
        this.performanceInsightBuilder = performanceInsightBuilder;
    }

    public PerformanceExportPayload buildPayload(PerfRsltEntity result, PerformanceThreadGroup threadDetail) {
        PerformanceManagementReport managementReport = performanceManagementReportBuilder.build(
                result.getRunSummary(),
                result.getThresholdResult(),
                result.getAnalysisSummary(),
                result.getErrorAnalysis(),
                result.getEnvironmentMetrics(),
                result.getBaselineComparison(),
                result.getSummary()
        );
        PerformanceInsightReport insightReport = result.getInsightReport() != null
                ? result.getInsightReport()
                : performanceInsightBuilder.build(
                result.getRunSummary(),
                result.getThresholdResult(),
                result.getAnalysisSummary(),
                result.getErrorAnalysis(),
                result.getEnvironmentMetrics(),
                result.getBaselineComparison(),
                result.getSummary(),
                threadDetail
        );
        PerformanceAiManagementReport aiManagementReport = result.getAiManagementReport() != null
                ? result.getAiManagementReport()
                : PerformanceAiManagementReport.notGenerated("AI report is not available for this performance result.");
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
                managementReport,
                insightReport,
                aiManagementReport,
                result.getSummary(),
                threadDetail
        );
    }

    public String buildCsv(PerformanceExportPayload payload) {
        StringBuilder csv = new StringBuilder();
        appendReportMetadata(csv, payload);
        appendValidationChecklist(csv, payload);
        appendRunSummary(csv, payload == null ? null : payload.runSummary());
        appendThresholdResult(csv, payload == null ? null : payload.thresholdResult());
        appendStepSummary(csv, payload == null ? null : payload.stepSummaries());
        appendErrorSummary(csv, payload);
        appendDecisionSummary(csv, payload);
        appendMetricInsights(csv, payload);
        appendRootCauseHints(csv, payload);
        appendAiActionPlan(csv, payload);
        appendAiObservability(csv, payload);
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
        csv.append('\n');
    }

    private void appendDecisionSummary(StringBuilder csv, PerformanceExportPayload payload) {
        PerformanceInsightReport insightReport = payload == null ? null : payload.insightReport();
        PerformanceAiManagementReport aiReport = payload == null ? null : payload.aiManagementReport();
        csv.append("Decision Summary\n");
        csv.append("Metric,Value\n");
        row(csv, "Release Readiness", insightReport == null ? null : insightReport.releaseReadiness());
        row(csv, "Anomaly Score", insightReport == null ? null : insightReport.anomalyScore());
        row(csv, "Anomaly Level", insightReport == null ? null : insightReport.anomalyLevel());
        row(csv, "Apdex", insightReport == null ? null : insightReport.apdexScore());
        row(csv, "SLO Compliance", insightReport == null ? null : insightReport.sloCompliancePercent());
        row(csv, "Regression Score", insightReport == null ? null : insightReport.regressionScore());
        row(csv, "Bottleneck Type", insightReport == null ? null : insightReport.bottleneckType());
        row(csv, "AI Generated", aiReport == null ? null : aiReport.generated());
        csv.append('\n');
    }

    private void appendMetricInsights(StringBuilder csv, PerformanceExportPayload payload) {
        csv.append("Metric Insights\n");
        csv.append("Metric,Severity,Actual,Expected,Explanation\n");
        PerformanceInsightReport insightReport = payload == null ? null : payload.insightReport();
        if (insightReport != null && insightReport.metricInsights() != null) {
            for (PerformanceMetricInsight insight : insightReport.metricInsights()) {
                if (insight != null) {
                    row(csv, insight.metric(), insight.severity(), insight.actual(), insight.expected(), insight.explanation());
                }
            }
        }
        csv.append('\n');
    }

    private void appendRootCauseHints(StringBuilder csv, PerformanceExportPayload payload) {
        csv.append("Root Cause Hints\n");
        csv.append("Severity,Category,Signal,Explanation,Recommendation\n");
        PerformanceInsightReport insightReport = payload == null ? null : payload.insightReport();
        if (insightReport != null && insightReport.rootCauseHints() != null) {
            for (PerformanceRootCauseHint hint : insightReport.rootCauseHints()) {
                if (hint != null) {
                    row(csv, hint.severity(), hint.category(), hint.signal(), hint.explanation(), hint.recommendation());
                }
            }
        }
        csv.append('\n');
    }

    private void appendAiActionPlan(StringBuilder csv, PerformanceExportPayload payload) {
        csv.append("AI Action Plan\n");
        csv.append("Priority,Title,Description,Related Step,Related Metric\n");
        PerformanceAiManagementReport aiReport = payload == null ? null : payload.aiManagementReport();
        if (aiReport != null && aiReport.recommendedActionPlan() != null) {
            for (PerformanceAiActionItem actionItem : aiReport.recommendedActionPlan()) {
                if (actionItem != null) {
                    row(csv, actionItem.priority(), actionItem.title(), actionItem.description(), actionItem.relatedStepName(), actionItem.relatedMetric());
                }
            }
        }
        csv.append('\n');
    }

    private void appendAiObservability(StringBuilder csv, PerformanceExportPayload payload) {
        csv.append("AI Observability\n");
        csv.append("Metric,Value\n");
        PerformanceAiManagementReport aiReport = payload == null ? null : payload.aiManagementReport();
        row(csv, "Schema Version", aiReport == null ? null : aiReport.schemaVersion());
        row(csv, "Generated By Version", aiReport == null ? null : aiReport.generatedByVersion());
        row(csv, "Model", aiReport == null ? null : aiReport.model());
        row(csv, "Duration Ms", aiReport == null ? null : aiReport.durationMs());
        row(csv, "Attempt Count", aiReport == null ? null : aiReport.attemptCount());
        row(csv, "Failure Reason", aiReport == null ? null : aiReport.failureReason());
        row(csv, "Validation Errors", aiReport == null || aiReport.validationErrors() == null ? null : String.join("; ", aiReport.validationErrors()));
        row(csv, "Prompt Hash", aiReport == null ? null : aiReport.promptHash());
        row(csv, "Input Summary Hash", aiReport == null ? null : aiReport.inputSummaryHash());
        row(csv, "Response Size", aiReport == null ? null : aiReport.responseSize());
        row(csv, "Prompt Tokens", aiReport == null ? null : aiReport.promptTokens());
        row(csv, "Completion Tokens", aiReport == null ? null : aiReport.completionTokens());
        row(csv, "Total Tokens", aiReport == null ? null : aiReport.totalTokens());
        csv.append('\n');
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
}
