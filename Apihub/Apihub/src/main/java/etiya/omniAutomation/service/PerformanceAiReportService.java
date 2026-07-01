package etiya.omniAutomation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.PerformanceAiActionItem;
import etiya.omniAutomation.business.dto.PerformanceAiManagementReport;
import etiya.omniAutomation.business.dto.PerformanceAnalysisSummary;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceInsightReport;
import etiya.omniAutomation.business.dto.PerformanceManagementReport;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PerformanceAiReportService {

    private static final int ERROR_MESSAGE_LIMIT = 300;

    private static final String SYSTEM_PROMPT = """
            Turkce bir performans testi yonetim raporu uret.
            Cevabin sadece tek bir JSON object olsun; markdown, aciklama veya kod blogu yazma.
            Verilen metriklerin disinda metrik uydurma.
            Deterministik releaseReadiness, riskLevel veya pass/fail kararini degistirme.
            Root-cause yorumlarini kesin hukum gibi degil, kanita dayali ve ihtiyatli ifade et.
            Ham request, response, token, credential, header, body veya log verisi yazma.
            JSON alanlari: executiveNarrative, technicalNarrative, rootCauseNarrative, recommendedActionPlan, releaseReadinessNarrative, limitations.
            recommendedActionPlan elemanlari: priority, title, description, relatedStepName, relatedMetric.
            """;

    private final PerformanceAiClient performanceAiClient;
    private final ObjectMapper objectMapper;
    private final PerformanceAiReportValidator performanceAiReportValidator;

    public PerformanceAiManagementReport generate(
            PerformanceManagementReport managementReport,
            PerformanceInsightReport insightReport,
            PerformanceRunSummary runSummary,
            PerformanceThresholdResult thresholdResult,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceErrorAnalysis errorAnalysis,
            PerformanceEnvironmentMetrics environmentMetrics,
            PerformanceComparisonResult baselineComparison,
            List<PerformanceSummary> stepSummaries
    ) {
        if (insightReport == null) {
            return PerformanceAiManagementReport.notGenerated("Insight report is required before AI narrative generation.");
        }

        String userPrompt;
        try {
            userPrompt = buildUserPrompt(
                    managementReport,
                    insightReport,
                    runSummary,
                    thresholdResult,
                    analysisSummary,
                    errorAnalysis,
                    environmentMetrics,
                    baselineComparison,
                    stepSummaries
            );
        } catch (RuntimeException | JsonProcessingException exception) {
            return PerformanceAiManagementReport.notGenerated("AI report generation failed: " + conciseMessage(exception));
        }

        String promptHash = sha256(SYSTEM_PROMPT + "\n" + userPrompt);
        String inputSummaryHash = sha256(userPrompt);
        String aiResponse;
        long startNanos = System.nanoTime();
        Long durationMs;
        try {
            aiResponse = performanceAiClient.complete(SYSTEM_PROMPT, userPrompt);
        } catch (RuntimeException exception) {
            durationMs = elapsedMs(startNanos);
            return PerformanceAiManagementReport.notGenerated(
                    "AI report generation failed: " + conciseMessage(exception),
                    "CLIENT_ERROR",
                    List.of(),
                    durationMs,
                    promptHash,
                    inputSummaryHash,
                    null
            );
        }
        durationMs = elapsedMs(startNanos);
        Integer responseSize = aiResponse == null ? 0 : aiResponse.getBytes(StandardCharsets.UTF_8).length;

        AiReportResponse parsed;
        try {
            parsed = objectMapper.readValue(aiResponse, AiReportResponse.class);
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return PerformanceAiManagementReport.notGenerated(
                    "AI response could not be parsed as JSON.",
                    "PARSE_ERROR",
                    List.of(),
                    durationMs,
                    promptHash,
                    inputSummaryHash,
                    responseSize
            );
        }

        PerformanceAiManagementReport aiReport = new PerformanceAiManagementReport(
                true,
                new Date(),
                performanceAiClient.modelName(),
                parsed.executiveNarrative(),
                parsed.technicalNarrative(),
                parsed.rootCauseNarrative(),
                safeList(parsed.recommendedActionPlan()),
                parsed.releaseReadinessNarrative(),
                parsed.limitations() == null ? List.of() : parsed.limitations(),
                null,
                PerformanceReportVersions.AI_REPORT_SCHEMA_VERSION,
                PerformanceReportVersions.AI_REPORT_GENERATOR_VERSION,
                durationMs,
                1,
                null,
                List.of(),
                promptHash,
                inputSummaryHash,
                responseSize,
                null,
                null,
                null
        );
        PerformanceAiValidationResult validation = performanceAiReportValidator.validate(
                aiReport,
                managementReport,
                insightReport,
                runSummary,
                thresholdResult
        );
        if (!validation.valid()) {
            return PerformanceAiManagementReport.notGenerated(
                    "AI output failed validation.",
                    "VALIDATION_FAILED",
                    validation.errors(),
                    durationMs,
                    promptHash,
                    inputSummaryHash,
                    responseSize
            );
        }
        return aiReport;
    }

    private String buildUserPrompt(
            PerformanceManagementReport managementReport,
            PerformanceInsightReport insightReport,
            PerformanceRunSummary runSummary,
            PerformanceThresholdResult thresholdResult,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceErrorAnalysis errorAnalysis,
            PerformanceEnvironmentMetrics environmentMetrics,
            PerformanceComparisonResult baselineComparison,
            List<PerformanceSummary> stepSummaries
    ) throws JsonProcessingException {
        Map<String, Object> promptPayload = Map.of(
                "managementReport", summarizeManagementReport(managementReport),
                "insightReport", insightReport,
                "runSummary", runSummary == null ? Map.of() : runSummary,
                "thresholdReasons", thresholdResult == null ? List.of() : safeList(thresholdResult.reasons()),
                "analysisSummary", summarizeAnalysis(analysisSummary),
                "errorAnalysis", summarizeErrors(errorAnalysis),
                "environmentMetrics", summarizeEnvironment(environmentMetrics),
                "baselineMetrics", baselineComparison == null ? List.of() : safeList(baselineComparison.metrics()),
                "topStepSummaries", topStepSummaries(stepSummaries)
        );
        return objectMapper.writeValueAsString(promptPayload);
    }

    private Map<String, Object> summarizeManagementReport(PerformanceManagementReport managementReport) {
        if (managementReport == null) {
            return Map.of();
        }
        return Map.of(
                "overallStatus", nullToEmpty(managementReport.overallStatus()),
                "riskLevel", managementReport.riskLevel() == null ? "" : managementReport.riskLevel().name(),
                "stepAssessmentSummary", nullToEmpty(managementReport.stepAssessmentSummary()),
                "slaSummary", safeList(managementReport.slaSummary())
        );
    }

    private Map<String, Object> summarizeAnalysis(PerformanceAnalysisSummary analysisSummary) {
        if (analysisSummary == null) {
            return Map.of();
        }
        return Map.of(
                "status", analysisSummary.status() == null ? "" : analysisSummary.status().name(),
                "problemStepName", nullToEmpty(analysisSummary.problemStepName()),
                "slowestStepName", nullToEmpty(analysisSummary.slowestStepName()),
                "highestP95StepName", nullToEmpty(analysisSummary.highestP95StepName()),
                "highestP99StepName", nullToEmpty(analysisSummary.highestP99StepName()),
                "highestErrorStepName", nullToEmpty(analysisSummary.highestErrorStepName()),
                "highestStdDeviationStepName", nullToEmpty(analysisSummary.highestStdDeviationStepName()),
                "summaryText", nullToEmpty(analysisSummary.summaryText()),
                "warnings", safeList(analysisSummary.warnings())
        );
    }

    private Map<String, Object> summarizeErrors(PerformanceErrorAnalysis errorAnalysis) {
        if (errorAnalysis == null) {
            return Map.of();
        }
        return Map.of(
                "totalErrorCount", errorAnalysis.totalErrorCount(),
                "errorRate", errorAnalysis.errorRate(),
                "errorsByType", safeList(errorAnalysis.errorsByType()).stream().limit(5).toList(),
                "errorsByStep", safeList(errorAnalysis.errorsByStep()).stream().limit(5).toList(),
                "lastErrorPresent", !isBlank(errorAnalysis.lastError()),
                "failedRequestCount", safeList(errorAnalysis.failedRequests()).size()
        );
    }

    private Map<String, Object> summarizeEnvironment(PerformanceEnvironmentMetrics environmentMetrics) {
        if (environmentMetrics == null) {
            return Map.of();
        }
        return Map.ofEntries(
                Map.entry("metricsAvailable", environmentMetrics.metricsAvailable()),
                Map.entry("message", nullToEmpty(environmentMetrics.message())),
                Map.entry("cpuAvgPercent", nullable(environmentMetrics.cpuAvgPercent())),
                Map.entry("cpuMaxPercent", nullable(environmentMetrics.cpuMaxPercent())),
                Map.entry("memoryAvgPercent", nullable(environmentMetrics.memoryAvgPercent())),
                Map.entry("memoryMaxPercent", nullable(environmentMetrics.memoryMaxPercent())),
                Map.entry("jvmHeapMaxPercent", nullable(environmentMetrics.jvmHeapMaxPercent())),
                Map.entry("gcTimeMs", nullable(environmentMetrics.gcTimeMs())),
                Map.entry("dbActiveConnectionMax", nullable(environmentMetrics.dbActiveConnectionMax())),
                Map.entry("dbConnectionPoolSize", nullable(environmentMetrics.dbConnectionPoolSize())),
                Map.entry("slowSqlCount", nullable(environmentMetrics.slowSqlCount())),
                Map.entry("http5xxCount", nullable(environmentMetrics.http5xxCount())),
                Map.entry("podRestartCount", nullable(environmentMetrics.podRestartCount())),
                Map.entry("warnings", safeList(environmentMetrics.warnings()))
        );
    }

    private List<Map<String, Object>> topStepSummaries(List<PerformanceSummary> stepSummaries) {
        if (stepSummaries == null || stepSummaries.isEmpty()) {
            return List.of();
        }
        return stepSummaries.stream()
                .filter(summary -> summary != null)
                .sorted((left, right) -> Double.compare(right.p99ElapsedTime(), left.p99ElapsedTime()))
                .limit(10)
                .map(summary -> Map.<String, Object>of(
                        "stepName", nullToEmpty(summary.stepName()),
                        "sampleCount", summary.sampleCount(),
                        "successCount", summary.successCount(),
                        "failureCount", summary.failureCount(),
                        "errorRate", summary.errorRate(),
                        "throughputPerSecond", summary.throughputPerSecond(),
                        "averageElapsedTime", summary.averageElapsedTime(),
                        "p95ElapsedTime", summary.p95ElapsedTime(),
                        "p99ElapsedTime", summary.p99ElapsedTime(),
                        "standardDeviation", summary.standardDeviation()
                ))
                .toList();
    }

    private String conciseMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() <= ERROR_MESSAGE_LIMIT ? message : message.substring(0, ERROR_MESSAGE_LIMIT);
    }

    private Long elapsedMs(long startNanos) {
        return Math.max(0, (System.nanoTime() - startNanos) / 1_000_000);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                hex.append(String.format(Locale.ROOT, "%02x", item & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Object nullable(Object value) {
        return value == null ? "" : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record AiReportResponse(
            String executiveNarrative,
            String technicalNarrative,
            String rootCauseNarrative,
            List<PerformanceAiActionItem> recommendedActionPlan,
            String releaseReadinessNarrative,
            List<String> limitations
    ) {
    }
}
