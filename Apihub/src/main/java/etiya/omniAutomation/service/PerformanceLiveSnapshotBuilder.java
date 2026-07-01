package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceLiveSnapshot;
import etiya.omniAutomation.business.dto.PerformanceResultItemDto;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.common.GeneralEnums;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public final class PerformanceLiveSnapshotBuilder {

    private PerformanceLiveSnapshotBuilder() {
    }

    public static PerformanceLiveSnapshot build(
            Long performanceResultId,
            GeneralEnums.PerformanceStatus status,
            PerformanceThreadGroup threadGroup,
            Date startedAt,
            Integer totalThreadCount,
            Integer durationSeconds,
            int activeFutureCount
    ) {
        if (threadGroup == null) {
            return new PerformanceLiveSnapshot(
                    performanceResultId,
                    status,
                    activeFutureCount,
                    totalThreadCount == null ? 0 : totalThreadCount,
                    0,
                    0,
                    0,
                    0,
                    0,
                    null,
                    null,
                    null,
                    elapsedTimeMs(startedAt),
                    estimatedRemainingMs(startedAt, durationSeconds),
                    null,
                    null,
                    List.of(),
                    false,
                    "Canlı metrik bilgisi alınamadı."
            );
        }

        List<PerformanceResultItemDto> samples = flatten(threadGroup);
        List<PerformanceResultItemDto> completedSamples = samples.stream()
                .filter(sample -> sample.getPerformanceItemStatus() != GeneralEnums.PerformanceStatus.RUNNING)
                .toList();
        List<Double> measuredValues = completedSamples.stream()
                .map(PerformanceResultItemDto::getElapsedTime)
                .filter(value -> value > 0)
                .sorted()
                .toList();

        long completed = completedSamples.size();
        long successful = completedSamples.stream()
                .filter(sample -> sample.getPerformanceItemStatus() == GeneralEnums.PerformanceStatus.COMPLETED)
                .count();
        long failed = completedSamples.stream()
                .filter(sample -> sample.getPerformanceItemStatus() == GeneralEnums.PerformanceStatus.FAILED)
                .count();
        long elapsedTimeMs = elapsedTimeMs(startedAt);
        double errorRate = completed == 0 ? 0 : (failed * 100.0) / completed;
        double throughput = elapsedTimeMs <= 0 ? 0 : completed / (elapsedTimeMs / 1000.0);
        Double average = measuredValues.isEmpty() ? null : measuredValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        Double p90 = measuredValues.isEmpty() ? null : percentile(measuredValues, 90);
        Double p95 = measuredValues.isEmpty() ? null : percentile(measuredValues, 95);
        String lastCompletedStep = latestFinished(completedSamples).map(PerformanceResultItemDto::getStepName).orElse(null);
        String lastError = latestFinished(completedSamples.stream()
                .filter(sample -> sample.getErrorMessage() != null && !sample.getErrorMessage().isBlank())
                .toList())
                .map(PerformanceResultItemDto::getErrorMessage)
                .orElse(null);

        return new PerformanceLiveSnapshot(
                performanceResultId,
                status,
                activeFutureCount,
                totalThreadCount == null ? threadGroup.groups().size() : totalThreadCount,
                completed,
                successful,
                failed,
                errorRate,
                throughput,
                average,
                p90,
                p95,
                elapsedTimeMs,
                estimatedRemainingMs(startedAt, durationSeconds),
                lastCompletedStep,
                lastError,
                liveWarnings(completed, errorRate, throughput, p95),
                true,
                null
        );
    }

    private static List<PerformanceResultItemDto> flatten(PerformanceThreadGroup threadGroup) {
        if (threadGroup.groups() == null) {
            return List.of();
        }
        return threadGroup.groups().stream()
                .filter(Objects::nonNull)
                .flatMap(group -> group.steps() == null ? java.util.stream.Stream.empty() : group.steps().stream())
                .filter(Objects::nonNull)
                .toList();
    }

    private static java.util.Optional<PerformanceResultItemDto> latestFinished(List<PerformanceResultItemDto> samples) {
        return samples.stream()
                .max(Comparator.comparingLong(sample -> sample.getFinishedAt() == null ? 0 : sample.getFinishedAt().getTime()));
    }

    private static long elapsedTimeMs(Date startedAt) {
        if (startedAt == null) {
            return 0;
        }
        return Math.max(0, new Date().getTime() - startedAt.getTime());
    }

    private static Long estimatedRemainingMs(Date startedAt, Integer durationSeconds) {
        if (startedAt == null || durationSeconds == null || durationSeconds <= 0) {
            return null;
        }
        return Math.max(0, (durationSeconds * 1000L) - elapsedTimeMs(startedAt));
    }

    private static double percentile(List<Double> values, int percentile) {
        int index = (int) Math.ceil((percentile / 100.0) * values.size()) - 1;
        index = Math.max(0, Math.min(index, values.size() - 1));
        return values.get(index);
    }

    private static List<String> liveWarnings(long completed, double errorRate, double throughput, Double p95) {
        if (completed == 0) {
            return List.of();
        }
        java.util.ArrayList<String> warnings = new java.util.ArrayList<>();
        if (errorRate > 1) {
            warnings.add("Canlı hata oranı threshold değerini aştı.");
        }
        if (p95 != null && p95 > 3000) {
            warnings.add("Canlı P95 değeri threshold değerini aştı.");
        }
        if (throughput < 20) {
            warnings.add("Canlı throughput beklenen değerin altında.");
        }
        return warnings;
    }
}
