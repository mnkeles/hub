package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.common.GeneralEnums;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PerformanceThresholdEvaluator {

    private PerformanceThresholdEvaluator() {
    }

    public static PerformanceThresholdResult evaluate(PerformanceRunSummary runSummary, PerformanceThresholdConfig config) {
        PerformanceThresholdConfig thresholds = config == null ? PerformanceThresholdConfig.defaults() : config;
        List<String> reasons = new ArrayList<>();

        if (runSummary == null) {
            reasons.add("Run summary is not available.");
            return new PerformanceThresholdResult(false, "COMPLETED - FAILED", reasons, thresholds);
        }

        if (runSummary.errorRate() > thresholds.maxErrorRatePercent()) {
            reasons.add(String.format(Locale.US,
                    "Error rate threshold exceeded. Expected <= %.2f%%, actual: %.2f%%",
                    thresholds.maxErrorRatePercent(),
                    runSummary.errorRate()));
        }
        if (runSummary.averageElapsedTime() > thresholds.maxAverageMs()) {
            reasons.add(String.format(Locale.US,
                    "Average response time threshold exceeded. Expected <= %.0f ms, actual: %.0f ms",
                    thresholds.maxAverageMs(),
                    runSummary.averageElapsedTime()));
        }
        if (runSummary.p95ElapsedTime() > thresholds.maxP95Ms()) {
            reasons.add(String.format(Locale.US,
                    "P95 threshold exceeded. Expected <= %.0f ms, actual: %.0f ms",
                    thresholds.maxP95Ms(),
                    runSummary.p95ElapsedTime()));
        }
        if (runSummary.p99ElapsedTime() > thresholds.maxP99Ms()) {
            reasons.add(String.format(Locale.US,
                    "P99 threshold exceeded. Expected <= %.0f ms, actual: %.0f ms",
                    thresholds.maxP99Ms(),
                    runSummary.p99ElapsedTime()));
        }
        if (runSummary.throughputPerSecond() < thresholds.minThroughputPerSecond()) {
            reasons.add(String.format(Locale.US,
                    "Throughput threshold not met. Expected >= %.2f req/s, actual: %.2f req/s",
                    thresholds.minThroughputPerSecond(),
                    runSummary.throughputPerSecond()));
        }

        boolean passed = reasons.isEmpty();
        return new PerformanceThresholdResult(
                passed,
                passed ? "COMPLETED - PASSED" : "COMPLETED - FAILED",
                reasons,
                thresholds
        );
    }

    public static GeneralEnums.PerformanceStatus finalStatus(PerformanceThresholdResult thresholdResult) {
        if (thresholdResult != null && thresholdResult.passed()) {
            return GeneralEnums.PerformanceStatus.COMPLETED_PASSED;
        }
        return GeneralEnums.PerformanceStatus.COMPLETED_FAILED;
    }
}
