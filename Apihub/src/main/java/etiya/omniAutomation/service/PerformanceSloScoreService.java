package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSloGrade;
import etiya.omniAutomation.business.dto.PerformanceSloMetricScore;
import etiya.omniAutomation.business.dto.PerformanceSloScore;
import etiya.omniAutomation.business.dto.PerformanceSloStatus;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class PerformanceSloScoreService {

    private static final double ERROR_WEIGHT = 30;
    private static final double P95_WEIGHT = 25;
    private static final double P99_WEIGHT = 15;
    private static final double AVERAGE_WEIGHT = 10;
    private static final double THROUGHPUT_WEIGHT = 10;
    private static final double BASELINE_WEIGHT = 10;

    public PerformanceSloScore calculate(PerformanceRunSummary runSummary, PerformanceThresholdConfig thresholds, PerformanceComparisonResult baselineComparison) {
        if (runSummary == null) {
            return null;
        }
        PerformanceThresholdConfig effectiveThresholds = thresholds == null ? PerformanceThresholdConfig.defaults() : thresholds;
        List<MetricInput> inputs = new ArrayList<>();
        addLowerBetter(inputs, "errorRate", runSummary.errorRate(), effectiveThresholds.maxErrorRatePercent(), ERROR_WEIGHT, "Hata orani");
        addLowerBetter(inputs, "p95ElapsedTime", runSummary.p95ElapsedTime(), effectiveThresholds.maxP95Ms(), P95_WEIGHT, "P95 gecikmesi");
        addLowerBetter(inputs, "p99ElapsedTime", runSummary.p99ElapsedTime(), effectiveThresholds.maxP99Ms(), P99_WEIGHT, "P99 gecikmesi");
        addLowerBetter(inputs, "averageElapsedTime", runSummary.averageElapsedTime(), effectiveThresholds.maxAverageMs(), AVERAGE_WEIGHT, "Ortalama gecikme");
        addHigherBetter(inputs, "throughputPerSecond", runSummary.throughputPerSecond(), effectiveThresholds.minThroughputPerSecond(), THROUGHPUT_WEIGHT, "Throughput");
        addBaseline(inputs, baselineComparison);

        double availableWeight = inputs.stream().mapToDouble(MetricInput::weight).sum();
        if (availableWeight <= 0) {
            return null;
        }

        List<PerformanceSloMetricScore> metricScores = new ArrayList<>();
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        double totalScore = 0;
        for (MetricInput input : inputs) {
            double maxScore = input.weight() * (100.0 / availableWeight);
            double score = clamp(input.ratio(), 0, 1) * maxScore;
            totalScore += score;
            metricScores.add(new PerformanceSloMetricScore(
                    input.metricName(),
                    round(score),
                    round(maxScore),
                    input.actualValue(),
                    input.targetValue(),
                    input.direction(),
                    input.message()
            ));
            collectNarrative(input, score, maxScore, strengths, weaknesses, recommendations);
        }

        int roundedScore = (int) Math.round(clamp(totalScore, 0, 100));
        PerformanceSloGrade grade = grade(roundedScore);
        return new PerformanceSloScore(
                roundedScore,
                grade,
                status(grade),
                metricScores,
                strengths.isEmpty() ? List.of("Performans sonucu sinirda; detay metrikleri inceleyin.") : strengths,
                weaknesses.isEmpty() ? List.of("Belirgin SLO zayifligi tespit edilmedi.") : weaknesses,
                recommendations.isEmpty() ? List.of("Sonucu baseline ve trendlerle takip edin.") : recommendations,
                new Date()
        );
    }

    private void addLowerBetter(List<MetricInput> inputs, String metricName, double actual, double target, double weight, String label) {
        if (target <= 0) {
            return;
        }
        double ratio = actual <= target ? 1 : 1 - ((actual - target) / target);
        inputs.add(new MetricInput(metricName, weight, ratio, actual, target, "LOWER_BETTER",
                label + " hedefi " + target + ", gerceklesen " + actual + "."));
    }

    private void addHigherBetter(List<MetricInput> inputs, String metricName, double actual, double target, double weight, String label) {
        if (target <= 0) {
            return;
        }
        double ratio = actual >= target ? 1 : (actual - (target * 0.5)) / (target * 0.5);
        inputs.add(new MetricInput(metricName, weight, ratio, actual, target, "HIGHER_BETTER",
                label + " hedefi " + target + ", gerceklesen " + actual + "."));
    }

    private void addBaseline(List<MetricInput> inputs, PerformanceComparisonResult baselineComparison) {
        if (baselineComparison == null || baselineComparison.metrics() == null || baselineComparison.metrics().isEmpty()) {
            return;
        }
        long regressions = baselineComparison.metrics().stream()
                .filter(Objects::nonNull)
                .filter(metric -> Boolean.FALSE.equals(metric.improvement()))
                .count();
        double ratio = regressions == 0 ? 1 : regressions == 1 ? 0.5 : 0;
        inputs.add(new MetricInput("baselineDeviation", BASELINE_WEIGHT, ratio, (double) regressions, 0.0,
                "LOWER_BETTER", regressions + " baseline regresyonu tespit edildi."));
    }

    private void collectNarrative(MetricInput input, double score, double maxScore, List<String> strengths, List<String> weaknesses, List<String> recommendations) {
        double ratio = maxScore <= 0 ? 0 : score / maxScore;
        if (ratio >= 0.9) {
            switch (input.metricName()) {
                case "errorRate" -> strengths.add("Hata orani hedef icinde.");
                case "p95ElapsedTime" -> strengths.add("P95 gecikmesi hedef icinde.");
                case "throughputPerSecond" -> strengths.add("Throughput hedefi karsiliyor.");
                case "baselineDeviation" -> strengths.add("Baseline karsisinda belirgin regresyon yok.");
                default -> {
                }
            }
        } else if (ratio < 0.6) {
            switch (input.metricName()) {
                case "errorRate" -> {
                    weaknesses.add("Hata orani hedefi asiyor.");
                    recommendations.add("Hata ureten adimlar icin log ve response govdelerini inceleyin.");
                }
                case "p95ElapsedTime", "p99ElapsedTime", "averageElapsedTime" -> {
                    weaknesses.add(input.metricName() + " hedefi asiyor.");
                    recommendations.add("Yavas adimlarda servis, sorgu ve bagimli sistem surelerini analiz edin.");
                }
                case "throughputPerSecond" -> {
                    weaknesses.add("Throughput hedefin altinda.");
                    recommendations.add("Thread, kaynak limiti ve servis kapasitesini birlikte kontrol edin.");
                }
                case "baselineDeviation" -> {
                    weaknesses.add("Baseline karsisinda performans regresyonu var.");
                    recommendations.add("Regresyon gorulen metrikleri onceki baseline kosumu ile karsilastirin.");
                }
                default -> {
                }
            }
        }
    }

    private PerformanceSloGrade grade(int score) {
        if (score >= 90) {
            return PerformanceSloGrade.A;
        }
        if (score >= 75) {
            return PerformanceSloGrade.B;
        }
        if (score >= 60) {
            return PerformanceSloGrade.C;
        }
        if (score >= 40) {
            return PerformanceSloGrade.D;
        }
        return PerformanceSloGrade.F;
    }

    private PerformanceSloStatus status(PerformanceSloGrade grade) {
        return switch (grade) {
            case A -> PerformanceSloStatus.EXCELLENT;
            case B -> PerformanceSloStatus.GOOD;
            case C -> PerformanceSloStatus.WARNING;
            case D, F -> PerformanceSloStatus.CRITICAL;
        };
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record MetricInput(
            String metricName,
            double weight,
            double ratio,
            Double actualValue,
            Double targetValue,
            String direction,
            String message
    ) {
    }
}
