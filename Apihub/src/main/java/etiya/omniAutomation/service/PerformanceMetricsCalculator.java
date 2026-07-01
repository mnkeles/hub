package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceResponseTimeBuckets;
import etiya.omniAutomation.business.dto.PerformanceResultItemDto;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.common.GeneralEnums;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PerformanceMetricsCalculator {

    private PerformanceMetricsCalculator() {
    }

    public static PerformanceRunSummary buildRunSummary(
            List<PerformanceResultItemDto> samples,
            Date startedAt,
            Date completedAt,
            int threadCount,
            int rampUpPeriod,
            GeneralEnums.PerformanceStatus status
    ) {
        List<PerformanceResultItemDto> safeSamples = safeSamples(samples);
        long totalDurationMs = calculateDurationMs(startedAt, completedAt);
        long totalSamples = safeSamples.size();
        long successfulSamples = countStatus(safeSamples, GeneralEnums.PerformanceStatus.COMPLETED);
        long failedSamples = countStatus(safeSamples, GeneralEnums.PerformanceStatus.FAILED);
        List<Double> measuredValues = measuredValues(safeSamples);
        List<PerformanceSummary> stepSummaries = buildStepSummariesByName(safeSamples, totalDurationMs);

        return new PerformanceRunSummary(
                status,
                startedAt,
                completedAt,
                totalDurationMs,
                threadCount,
                rampUpPeriod,
                totalSamples,
                successfulSamples,
                failedSamples,
                calculateErrorRate(totalSamples, failedSamples),
                calculateThroughput(totalSamples, totalDurationMs),
                average(measuredValues),
                min(measuredValues),
                max(measuredValues),
                percentile(measuredValues, 50),
                percentile(measuredValues, 90),
                percentile(measuredValues, 95),
                percentile(measuredValues, 99),
                slowestStepName(stepSummaries)
        );
    }

    public static List<PerformanceSummary> buildStepSummaries(
            Map<Long, String> stepNamesById,
            Map<Long, List<PerformanceResultItemDto>> samplesByStep,
            long totalDurationMs
    ) {
        if (samplesByStep == null || samplesByStep.isEmpty()) {
            return List.of();
        }

        return samplesByStep.entrySet().stream()
                .map(entry -> buildStepSummary(
                        stepNamesById == null ? null : stepNamesById.get(entry.getKey()),
                        entry.getValue(),
                        totalDurationMs
                ))
                .toList();
    }

    private static List<PerformanceSummary> buildStepSummariesByName(List<PerformanceResultItemDto> samples, long totalDurationMs) {
        return samples.stream()
                .collect(java.util.stream.Collectors.groupingBy(PerformanceMetricsCalculator::sampleStepName))
                .entrySet()
                .stream()
                .map(entry -> buildStepSummary(entry.getKey(), entry.getValue(), totalDurationMs))
                .toList();
    }

    private static PerformanceSummary buildStepSummary(String stepName, List<PerformanceResultItemDto> samples, long totalDurationMs) {
        List<PerformanceResultItemDto> safeSamples = safeSamples(samples);
        long sampleCount = safeSamples.size();
        long successCount = countStatus(safeSamples, GeneralEnums.PerformanceStatus.COMPLETED);
        long failureCount = countStatus(safeSamples, GeneralEnums.PerformanceStatus.FAILED);
        List<Double> measuredValues = measuredValues(safeSamples);

        return new PerformanceSummary(
                stepName == null || stepName.isBlank() ? "Unknown" : stepName,
                max(measuredValues),
                min(measuredValues),
                average(measuredValues),
                sampleCount,
                successCount,
                failureCount,
                calculateErrorRate(sampleCount, failureCount),
                calculateThroughput(sampleCount, totalDurationMs),
                percentile(measuredValues, 50),
                percentile(measuredValues, 90),
                percentile(measuredValues, 95),
                percentile(measuredValues, 99),
                standardDeviation(measuredValues),
                lastError(safeSamples),
                responseTimeBuckets(measuredValues)
        );
    }

    private static List<PerformanceResultItemDto> safeSamples(List<PerformanceResultItemDto> samples) {
        if (samples == null || samples.isEmpty()) {
            return List.of();
        }
        return samples.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private static long calculateDurationMs(Date startedAt, Date completedAt) {
        if (startedAt == null || completedAt == null) {
            return 0;
        }
        return Math.max(0, completedAt.getTime() - startedAt.getTime());
    }

    private static long countStatus(List<PerformanceResultItemDto> samples, GeneralEnums.PerformanceStatus status) {
        return samples.stream()
                .filter(sample -> sample.getPerformanceItemStatus() == status)
                .count();
    }

    private static List<Double> measuredValues(List<PerformanceResultItemDto> samples) {
        return samples.stream()
                .map(PerformanceResultItemDto::getElapsedTime)
                .filter(value -> value > 0)
                .sorted()
                .toList();
    }

    private static double calculateErrorRate(long sampleCount, long failedCount) {
        if (sampleCount == 0) {
            return 0;
        }
        return (failedCount * 100.0) / sampleCount;
    }

    private static double calculateThroughput(long sampleCount, long totalDurationMs) {
        if (sampleCount == 0 || totalDurationMs <= 0) {
            return 0;
        }
        return sampleCount / (totalDurationMs / 1000.0);
    }

    private static double average(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);
    }

    private static double min(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        return values.get(0);
    }

    private static double max(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        return values.get(values.size() - 1);
    }

    private static double percentile(List<Double> values, int percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        index = Math.max(0, Math.min(index, values.size() - 1));
        return values.get(index);
    }

    private static double standardDeviation(List<Double> values) {
        if (values.isEmpty()) {
            return 0;
        }
        double average = average(values);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - average, 2))
                .sum() / values.size();
        return Math.sqrt(variance);
    }

    private static String lastError(List<PerformanceResultItemDto> samples) {
        List<PerformanceResultItemDto> reversed = new ArrayList<>(samples);
        java.util.Collections.reverse(reversed);
        return reversed.stream()
                .map(PerformanceResultItemDto::getErrorMessage)
                .filter(error -> error != null && !error.isBlank())
                .findFirst()
                .orElse(null);
    }

    private static PerformanceResponseTimeBuckets responseTimeBuckets(List<Double> measuredValues) {
        long under500ms = 0;
        long from500msTo1s = 0;
        long from1sTo3s = 0;
        long over3s = 0;

        for (double value : measuredValues) {
            if (value < 500) {
                under500ms++;
            } else if (value < 1000) {
                from500msTo1s++;
            } else if (value < 3000) {
                from1sTo3s++;
            } else {
                over3s++;
            }
        }

        return new PerformanceResponseTimeBuckets(under500ms, from500msTo1s, from1sTo3s, over3s);
    }

    private static String slowestStepName(List<PerformanceSummary> stepSummaries) {
        return stepSummaries.stream()
                .max(Comparator.comparingDouble(PerformanceSummary::averageElapsedTime))
                .map(PerformanceSummary::stepName)
                .orElse(null);
    }

    private static String sampleStepName(PerformanceResultItemDto sample) {
        String stepName = sample.getStepName();
        return stepName == null || stepName.isBlank() ? "Unknown" : stepName;
    }
}
