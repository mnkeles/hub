package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAnalysisSummary;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceErrorTypeCount;
import etiya.omniAutomation.business.dto.PerformanceFailedRequest;
import etiya.omniAutomation.business.dto.PerformanceResultItemDto;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceStepErrorCount;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.common.GeneralEnums;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class PerformanceAnalysisBuilder {

    private PerformanceAnalysisBuilder() {
    }

    public static PerformanceAnalysisSummary buildAnalysis(
            PerformanceRunSummary runSummary,
            List<PerformanceSummary> stepSummaries,
            PerformanceThresholdResult thresholdResult
    ) {
        List<PerformanceSummary> summaries = safeSummaries(stepSummaries);
        GeneralEnums.PerformanceStatus status = PerformanceThresholdEvaluator.finalStatus(thresholdResult);

        if (summaries.isEmpty()) {
            return new PerformanceAnalysisSummary(
                    status,
                    thresholdResult,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "Test sonucu analiz edildi ancak adım özeti bulunamadı.",
                    List.of()
            );
        }

        PerformanceSummary slowest = maxBy(summaries, PerformanceSummary::averageElapsedTime);
        PerformanceSummary highestP95 = maxBy(summaries, PerformanceSummary::p95ElapsedTime);
        PerformanceSummary highestP99 = maxBy(summaries, PerformanceSummary::p99ElapsedTime);
        PerformanceSummary highestStdDeviation = maxBy(summaries, PerformanceSummary::standardDeviation);
        PerformanceSummary highestError = summaries.stream()
                .filter(summary -> summary.failureCount() > 0)
                .max(Comparator.comparingLong(PerformanceSummary::failureCount))
                .orElse(null);
        PerformanceSummary problemStep = problemStep(summaries, thresholdResult);
        String problemStepName = problemStep == null ? null : problemStep.stepName();
        String summaryText = buildSummaryText(thresholdResult, slowest, problemStepName);

        return new PerformanceAnalysisSummary(
                status,
                thresholdResult,
                problemStepName,
                slowest == null ? null : slowest.stepName(),
                highestP95 == null ? null : highestP95.stepName(),
                highestP99 == null ? null : highestP99.stepName(),
                highestError == null ? null : highestError.stepName(),
                highestStdDeviation == null ? null : highestStdDeviation.stepName(),
                summaryText,
                List.of()
        );
    }

    public static PerformanceErrorAnalysis buildErrorAnalysis(
            PerformanceRunSummary runSummary,
            List<PerformanceSummary> stepSummaries,
            PerformanceThreadGroup threadDetail
    ) {
        List<PerformanceFailedRequest> failedRequests = flatten(threadDetail).stream()
                .filter(PerformanceAnalysisBuilder::isFailedRequest)
                .map(item -> new PerformanceFailedRequest(
                        item.getThreadNumber(),
                        item.getStepName(),
                        item.getElapsedTime(),
                        classifyError(item.getErrorMessage()),
                        item.getErrorMessage()
                ))
                .toList();

        Map<String, Long> byType = failedRequests.stream()
                .collect(Collectors.groupingBy(PerformanceFailedRequest::errorType, LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> byStep = failedRequests.stream()
                .collect(Collectors.groupingBy(request -> safeStepName(request.stepName()), LinkedHashMap::new, Collectors.counting()));

        List<PerformanceErrorTypeCount> errorsByType = byType.entrySet().stream()
                .map(entry -> new PerformanceErrorTypeCount(entry.getKey(), entry.getValue()))
                .toList();
        List<PerformanceStepErrorCount> errorsByStep = byStep.entrySet().stream()
                .map(entry -> new PerformanceStepErrorCount(entry.getKey(), entry.getValue()))
                .toList();
        String lastError = lastError(flatten(threadDetail));
        long totalErrorCount = failedRequests.size();
        double errorRate = runSummary == null ? 0 : runSummary.errorRate();

        return new PerformanceErrorAnalysis(totalErrorCount, errorRate, errorsByType, errorsByStep, lastError, failedRequests);
    }

    public static PerformanceEnvironmentMetrics unavailableEnvironmentMetrics() {
        return new PerformanceEnvironmentMetrics(
                false,
                "Ortam metrikleri için entegrasyon bulunamadı.",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of()
        );
    }

    public static String classifyError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "Other";
        }

        String value = errorMessage.toLowerCase(Locale.ROOT);
        if (value.matches(".*\\b4\\d\\d\\b.*") || value.contains("http 4")) {
            return "HTTP 4xx";
        }
        if (value.matches(".*\\b5\\d\\d\\b.*") || value.contains("http 5")) {
            return "HTTP 5xx";
        }
        if (value.contains("timeout") || value.contains("timed out") || value.contains("read timed")) {
            return "Timeout";
        }
        if (value.contains("assert") || value.contains("validation") || value.contains("expected")) {
            return "Assertion Failed";
        }
        if (value.contains("connection") || value.contains("refused") || value.contains("reset") || value.contains("socket")) {
            return "Connection Error";
        }
        if (value.contains("sql") || value.contains("database") || value.contains("jdbc") || value.contains("hibernate")) {
            return "Database Error";
        }
        return "Other";
    }

    private static PerformanceSummary problemStep(List<PerformanceSummary> summaries, PerformanceThresholdResult thresholdResult) {
        PerformanceThresholdConfig thresholds = thresholdResult == null || thresholdResult.thresholds() == null
                ? PerformanceThresholdConfig.defaults()
                : thresholdResult.thresholds();
        PerformanceSummary highestAverage = maxBy(summaries, PerformanceSummary::averageElapsedTime);
        PerformanceSummary highestStdDeviation = maxBy(summaries, PerformanceSummary::standardDeviation);
        Map<PerformanceSummary, Integer> scores = new LinkedHashMap<>();

        for (PerformanceSummary summary : summaries) {
            int score = 0;
            if (summary.p95ElapsedTime() > thresholds.maxP95Ms()) {
                score += 4;
            }
            if (summary.p99ElapsedTime() > thresholds.maxP99Ms()) {
                score += 4;
            }
            if (summary.errorRate() > 0) {
                score += 3;
            }
            if (summary == highestAverage) {
                score += 2;
            }
            if (summary == highestStdDeviation) {
                score += 1;
            }
            scores.put(summary, score);
        }

        return scores.entrySet().stream()
                .max(Comparator
                        .<Map.Entry<PerformanceSummary, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(entry -> entry.getKey().p99ElapsedTime())
                        .thenComparing(entry -> entry.getKey().failureCount()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static String buildSummaryText(PerformanceThresholdResult thresholdResult, PerformanceSummary slowest, String problemStepName) {
        String slowestStepName = slowest == null ? "-" : slowest.stepName();
        if (thresholdResult != null && thresholdResult.passed()) {
            return String.format("Test tanımlı threshold değerleri içinde tamamlandı. En yavaş adım %s olarak görünüyor.", slowestStepName);
        }
        return String.format("Bu testte en problemli adım %s olarak görünüyor. Threshold aşımı veya hata yoğunluğu bu adımı optimizasyon için öncelikli hale getiriyor.",
                problemStepName == null ? slowestStepName : problemStepName);
    }

    private static List<PerformanceSummary> safeSummaries(List<PerformanceSummary> stepSummaries) {
        if (stepSummaries == null || stepSummaries.isEmpty()) {
            return List.of();
        }
        return stepSummaries.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static PerformanceSummary maxBy(List<PerformanceSummary> summaries, java.util.function.ToDoubleFunction<PerformanceSummary> extractor) {
        return summaries.stream()
                .max(Comparator.comparingDouble(extractor))
                .orElse(null);
    }

    private static List<PerformanceResultItemDto> flatten(PerformanceThreadGroup threadDetail) {
        if (threadDetail == null || threadDetail.groups() == null) {
            return List.of();
        }
        return threadDetail.groups().stream()
                .filter(Objects::nonNull)
                .flatMap(group -> group.steps() == null ? java.util.stream.Stream.empty() : group.steps().stream())
                .filter(Objects::nonNull)
                .toList();
    }

    private static boolean isFailedRequest(PerformanceResultItemDto item) {
        return item.getPerformanceItemStatus() == GeneralEnums.PerformanceStatus.FAILED
                || (item.getErrorMessage() != null && !item.getErrorMessage().isBlank());
    }

    private static String lastError(List<PerformanceResultItemDto> items) {
        List<PerformanceResultItemDto> reversed = new ArrayList<>(items);
        java.util.Collections.reverse(reversed);
        return reversed.stream()
                .map(PerformanceResultItemDto::getErrorMessage)
                .filter(error -> error != null && !error.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static String safeStepName(String stepName) {
        return stepName == null || stepName.isBlank() ? "Unknown" : stepName;
    }
}
