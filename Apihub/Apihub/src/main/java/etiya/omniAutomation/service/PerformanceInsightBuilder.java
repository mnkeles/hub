package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAnalysisSummary;
import etiya.omniAutomation.business.dto.PerformanceAnomalyLevel;
import etiya.omniAutomation.business.dto.PerformanceBottleneckType;
import etiya.omniAutomation.business.dto.PerformanceComparisonMetric;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceInsightReport;
import etiya.omniAutomation.business.dto.PerformanceInsightSeverity;
import etiya.omniAutomation.business.dto.PerformanceMetricInsight;
import etiya.omniAutomation.business.dto.PerformanceReleaseReadiness;
import etiya.omniAutomation.business.dto.PerformanceResponseTimeBuckets;
import etiya.omniAutomation.business.dto.PerformanceRootCauseHint;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceStepInsight;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThread;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.common.GeneralEnums;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class PerformanceInsightBuilder {

    private static final Set<String> CRITICAL_REGRESSION_METRICS = Set.of(
            "p99",
            "p95",
            "averageResponseTime",
            "errorRate",
            "throughput"
    );

    public PerformanceInsightReport build(
            PerformanceRunSummary runSummary,
            PerformanceThresholdResult thresholdResult,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceErrorAnalysis errorAnalysis,
            PerformanceEnvironmentMetrics environmentMetrics,
            PerformanceComparisonResult baselineComparison,
            List<PerformanceSummary> stepSummaries,
            PerformanceThreadGroup threadDetail
    ) {
        PerformanceThresholdConfig thresholds = thresholds(thresholdResult);
        Double apdexScore = apdexScore(threadDetail, stepSummaries, thresholds);
        boolean apdexEstimated = apdexScore != null && !hasMeasuredSamples(threadDetail);
        double sloCompliancePercent = sloCompliancePercent(runSummary, thresholds);
        boolean regressionAvailable = hasBaselineMetrics(baselineComparison);
        Double regressionScore = regressionAvailable ? regressionScore(baselineComparison) : null;
        double anomalyScore = anomalyScore(runSummary, thresholds, stepSummaries, baselineComparison);
        PerformanceAnomalyLevel anomalyLevel = anomalyLevel(anomalyScore);
        PerformanceBottleneckType bottleneckType = bottleneckType(runSummary, thresholds, stepSummaries, environmentMetrics);
        PerformanceReleaseReadiness releaseReadiness = releaseReadiness(runSummary, thresholdResult, anomalyLevel, regressionScore, environmentMetrics);

        return new PerformanceInsightReport(
                anomalyScore,
                anomalyLevel,
                regressionScore,
                regressionAvailable,
                apdexScore,
                apdexEstimated,
                sloCompliancePercent,
                bottleneckType,
                releaseReadiness,
                rootCauseHints(environmentMetrics),
                metricInsights(runSummary, thresholds),
                stepInsights(stepSummaries, analysisSummary, thresholds),
                PerformanceReportVersions.INSIGHT_SCHEMA_VERSION,
                PerformanceReportVersions.INSIGHT_GENERATOR_VERSION
        );
    }

    private PerformanceThresholdConfig thresholds(PerformanceThresholdResult thresholdResult) {
        return thresholdResult == null || thresholdResult.thresholds() == null
                ? PerformanceThresholdConfig.defaults()
                : thresholdResult.thresholds();
    }

    private Double apdexScore(
            PerformanceThreadGroup threadDetail,
            List<PerformanceSummary> stepSummaries,
            PerformanceThresholdConfig thresholds
    ) {
        List<Double> samples = measuredSamples(threadDetail);
        if (!samples.isEmpty()) {
            return apdexFromValues(samples, thresholds.maxAverageMs());
        }
        return estimatedApdexFromBuckets(stepSummaries, thresholds.maxAverageMs());
    }

    private List<Double> measuredSamples(PerformanceThreadGroup threadDetail) {
        if (threadDetail == null || threadDetail.groups() == null || threadDetail.groups().isEmpty()) {
            return List.of();
        }
        return threadDetail.groups().stream()
                .filter(Objects::nonNull)
                .map(PerformanceThread::steps)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .map(item -> item.getElapsedTime())
                .filter(value -> value > 0)
                .toList();
    }

    private boolean hasMeasuredSamples(PerformanceThreadGroup threadDetail) {
        return !measuredSamples(threadDetail).isEmpty();
    }

    private double apdexFromValues(List<Double> samples, double thresholdMs) {
        double t = thresholdMs > 0 ? thresholdMs : PerformanceThresholdConfig.defaults().maxAverageMs();
        long satisfied = samples.stream().filter(value -> value <= t).count();
        long tolerating = samples.stream().filter(value -> value > t && value <= t * 4).count();
        return (satisfied + tolerating / 2.0) / samples.size();
    }

    private Double estimatedApdexFromBuckets(List<PerformanceSummary> stepSummaries, double thresholdMs) {
        if (stepSummaries == null || stepSummaries.isEmpty()) {
            return null;
        }
        long satisfied = 0;
        long tolerating = 0;
        long total = 0;
        for (PerformanceSummary summary : stepSummaries) {
            if (summary == null || summary.responseTimeBuckets() == null) {
                continue;
            }
            PerformanceResponseTimeBuckets buckets = summary.responseTimeBuckets();
            total += buckets.under500ms() + buckets.from500msTo1s() + buckets.from1sTo3s() + buckets.over3s();
            if (thresholdMs >= 1000) {
                satisfied += buckets.under500ms() + buckets.from500msTo1s();
                tolerating += buckets.from1sTo3s();
            } else {
                satisfied += buckets.under500ms();
                tolerating += buckets.from500msTo1s() + buckets.from1sTo3s();
            }
        }
        if (total == 0) {
            return null;
        }
        return (satisfied + tolerating / 2.0) / total;
    }

    private double sloCompliancePercent(PerformanceRunSummary runSummary, PerformanceThresholdConfig thresholds) {
        if (runSummary == null) {
            return 0;
        }
        int passed = 0;
        if (runSummary.errorRate() <= thresholds.maxErrorRatePercent()) {
            passed++;
        }
        if (runSummary.averageElapsedTime() <= thresholds.maxAverageMs()) {
            passed++;
        }
        if (runSummary.p95ElapsedTime() <= thresholds.maxP95Ms()) {
            passed++;
        }
        if (runSummary.p99ElapsedTime() <= thresholds.maxP99Ms()) {
            passed++;
        }
        if (runSummary.throughputPerSecond() >= thresholds.minThroughputPerSecond()) {
            passed++;
        }
        return (passed / 5.0) * 100.0;
    }

    private boolean hasBaselineMetrics(PerformanceComparisonResult baselineComparison) {
        return baselineComparison != null
                && baselineComparison.metrics() != null
                && !baselineComparison.metrics().isEmpty();
    }

    private double regressionScore(PerformanceComparisonResult baselineComparison) {
        double score = 100;
        for (PerformanceComparisonMetric metric : safeMetrics(baselineComparison)) {
            if (!Boolean.FALSE.equals(metric.improvement())) {
                continue;
            }
            score -= regressionPenalty(metric.metricName());
        }
        return Math.max(0, score);
    }

    private double regressionPenalty(String metricName) {
        if ("p99".equals(metricName)) {
            return 25;
        }
        if ("p95".equals(metricName)) {
            return 20;
        }
        if ("averageResponseTime".equals(metricName)) {
            return 15;
        }
        if ("errorRate".equals(metricName)) {
            return 25;
        }
        if ("throughput".equals(metricName)) {
            return 15;
        }
        return 0;
    }

    private double anomalyScore(
            PerformanceRunSummary runSummary,
            PerformanceThresholdConfig thresholds,
            List<PerformanceSummary> stepSummaries,
            PerformanceComparisonResult baselineComparison
    ) {
        double score = 0;
        if (runSummary != null) {
            if (runSummary.p99ElapsedTime() > thresholds.maxP99Ms() * 2) {
                score += 30;
            }
            if (runSummary.p95ElapsedTime() > thresholds.maxP95Ms()) {
                score += 20;
            }
            if (runSummary.errorRate() > thresholds.maxErrorRatePercent()) {
                score += 25;
            }
            if (runSummary.throughputPerSecond() < thresholds.minThroughputPerSecond()) {
                score += 15;
            }
        }
        if (hasUnstableStep(stepSummaries)) {
            score += 10;
        }
        if (hasCriticalRegression(baselineComparison)) {
            score += 20;
        }
        return Math.min(100, score);
    }

    private PerformanceAnomalyLevel anomalyLevel(double anomalyScore) {
        if (anomalyScore >= 75) {
            return PerformanceAnomalyLevel.CRITICAL;
        }
        if (anomalyScore >= 50) {
            return PerformanceAnomalyLevel.ANOMALOUS;
        }
        if (anomalyScore >= 25) {
            return PerformanceAnomalyLevel.WATCH;
        }
        return PerformanceAnomalyLevel.NORMAL;
    }

    private PerformanceBottleneckType bottleneckType(
            PerformanceRunSummary runSummary,
            PerformanceThresholdConfig thresholds,
            List<PerformanceSummary> stepSummaries,
            PerformanceEnvironmentMetrics environmentMetrics
    ) {
        boolean error = runSummary != null && runSummary.errorRate() > thresholds.maxErrorRatePercent();
        boolean latency = runSummary != null
                && (runSummary.p95ElapsedTime() > thresholds.maxP95Ms()
                || runSummary.p99ElapsedTime() > thresholds.maxP99Ms());
        boolean throughput = runSummary != null && runSummary.throughputPerSecond() < thresholds.minThroughputPerSecond();
        boolean instability = hasUnstableStep(stepSummaries);
        boolean environment = hasEnvironmentWarnings(environmentMetrics) && latency;

        if (environment) {
            return countSignals(error, latency, throughput, instability) > 1
                    ? PerformanceBottleneckType.MIXED
                    : PerformanceBottleneckType.ENVIRONMENT;
        }
        int signals = countSignals(error, latency, throughput, instability);
        if (signals > 1) {
            return PerformanceBottleneckType.MIXED;
        }
        if (error) {
            return PerformanceBottleneckType.ERROR;
        }
        if (latency) {
            return PerformanceBottleneckType.LATENCY;
        }
        if (throughput) {
            return PerformanceBottleneckType.THROUGHPUT;
        }
        if (instability) {
            return PerformanceBottleneckType.INSTABILITY;
        }
        return PerformanceBottleneckType.NONE;
    }

    private int countSignals(boolean... signals) {
        int count = 0;
        for (boolean signal : signals) {
            if (signal) {
                count++;
            }
        }
        return count;
    }

    private PerformanceReleaseReadiness releaseReadiness(
            PerformanceRunSummary runSummary,
            PerformanceThresholdResult thresholdResult,
            PerformanceAnomalyLevel anomalyLevel,
            Double regressionScore,
            PerformanceEnvironmentMetrics environmentMetrics
    ) {
        if (runSummary == null || isNonReleaseStatus(runSummary.status())) {
            return PerformanceReleaseReadiness.UNKNOWN;
        }
        if ((thresholdResult != null && !thresholdResult.passed())
                || anomalyLevel == PerformanceAnomalyLevel.CRITICAL
                || (regressionScore != null && regressionScore < 70)) {
            return PerformanceReleaseReadiness.BLOCKED;
        }
        if (anomalyLevel == PerformanceAnomalyLevel.WATCH
                || anomalyLevel == PerformanceAnomalyLevel.ANOMALOUS
                || hasEnvironmentWarnings(environmentMetrics)
                || (regressionScore != null && regressionScore < 90)) {
            return PerformanceReleaseReadiness.CONDITIONAL;
        }
        return PerformanceReleaseReadiness.READY;
    }

    private boolean isNonReleaseStatus(GeneralEnums.PerformanceStatus status) {
        return status == GeneralEnums.PerformanceStatus.RUNNING
                || status == GeneralEnums.PerformanceStatus.STOPPING
                || status == GeneralEnums.PerformanceStatus.STOPPED
                || status == GeneralEnums.PerformanceStatus.ERROR;
    }

    private List<PerformanceMetricInsight> metricInsights(PerformanceRunSummary runSummary, PerformanceThresholdConfig thresholds) {
        if (runSummary == null) {
            return List.of();
        }
        List<PerformanceMetricInsight> insights = new ArrayList<>();
        addMaxMetricInsight(insights, "Hata oranı", runSummary.errorRate(), thresholds.maxErrorRatePercent(), "%", 0.70);
        addMaxMetricInsight(insights, "Ortalama yanıt süresi", runSummary.averageElapsedTime(), thresholds.maxAverageMs(), " ms", 0.80);
        addMaxMetricInsight(insights, "P95", runSummary.p95ElapsedTime(), thresholds.maxP95Ms(), " ms", 0.80);
        addMaxMetricInsight(insights, "P99", runSummary.p99ElapsedTime(), thresholds.maxP99Ms(), " ms", 0.80);
        if (runSummary.throughputPerSecond() < thresholds.minThroughputPerSecond()) {
            insights.add(new PerformanceMetricInsight(
                    "Throughput",
                    PerformanceInsightSeverity.HIGH,
                    formatNumber(runSummary.throughputPerSecond()) + " req/s",
                    ">= " + formatNumber(thresholds.minThroughputPerSecond()) + " req/s",
                    "Throughput hedefin altında."
            ));
        } else if (runSummary.throughputPerSecond() <= thresholds.minThroughputPerSecond() * 1.20) {
            insights.add(new PerformanceMetricInsight(
                    "Throughput",
                    PerformanceInsightSeverity.WARNING,
                    formatNumber(runSummary.throughputPerSecond()) + " req/s",
                    ">= " + formatNumber(thresholds.minThroughputPerSecond()) + " req/s",
                    "Throughput hedefe yakın seviyede."
            ));
        }
        return insights;
    }

    private void addMaxMetricInsight(
            List<PerformanceMetricInsight> insights,
            String metric,
            double actual,
            double expectedMax,
            String suffix,
            double nearRatio
    ) {
        if (actual > expectedMax) {
            insights.add(new PerformanceMetricInsight(
                    metric,
                    PerformanceInsightSeverity.HIGH,
                    formatNumber(actual) + suffix,
                    "<= " + formatNumber(expectedMax) + suffix,
                    metric + " hedefin üzerinde."
            ));
        } else if (actual >= expectedMax * nearRatio) {
            insights.add(new PerformanceMetricInsight(
                    metric,
                    PerformanceInsightSeverity.WARNING,
                    formatNumber(actual) + suffix,
                    "<= " + formatNumber(expectedMax) + suffix,
                    metric + " hedefe yakın."
            ));
        }
    }

    private List<PerformanceStepInsight> stepInsights(
            List<PerformanceSummary> stepSummaries,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceThresholdConfig thresholds
    ) {
        if (stepSummaries == null || stepSummaries.isEmpty()) {
            return List.of();
        }
        return stepSummaries.stream()
                .filter(Objects::nonNull)
                .filter(summary -> stepHasInsight(summary, analysisSummary, thresholds))
                .sorted(Comparator.comparingDouble(PerformanceSummary::p99ElapsedTime).reversed())
                .map(summary -> stepInsight(summary, thresholds))
                .toList();
    }

    private boolean stepHasInsight(
            PerformanceSummary summary,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceThresholdConfig thresholds
    ) {
        return summary.errorRate() > thresholds.maxErrorRatePercent()
                || summary.averageElapsedTime() > thresholds.maxAverageMs()
                || summary.p95ElapsedTime() > thresholds.maxP95Ms()
                || summary.p99ElapsedTime() > thresholds.maxP99Ms()
                || isHighlightedStep(summary.stepName(), analysisSummary);
    }

    private boolean isHighlightedStep(String stepName, PerformanceAnalysisSummary analysisSummary) {
        if (stepName == null || analysisSummary == null) {
            return false;
        }
        return Objects.equals(stepName, analysisSummary.problemStepName())
                || Objects.equals(stepName, analysisSummary.slowestStepName())
                || Objects.equals(stepName, analysisSummary.highestErrorStepName());
    }

    private PerformanceStepInsight stepInsight(PerformanceSummary summary, PerformanceThresholdConfig thresholds) {
        if (summary.errorRate() > thresholds.maxErrorRatePercent()) {
            return new PerformanceStepInsight(summary.stepName(), PerformanceInsightSeverity.HIGH, PerformanceBottleneckType.ERROR,
                    "Hata oranı", formatNumber(summary.errorRate()) + "%", "<= " + formatNumber(thresholds.maxErrorRatePercent()) + "%",
                    "Bu adımda hata oranı hedefin üzerinde.");
        }
        if (summary.p99ElapsedTime() > thresholds.maxP99Ms()) {
            return new PerformanceStepInsight(summary.stepName(), PerformanceInsightSeverity.HIGH, PerformanceBottleneckType.LATENCY,
                    "P99", formatNumber(summary.p99ElapsedTime()) + " ms", "<= " + formatNumber(thresholds.maxP99Ms()) + " ms",
                    "Bu adım uç gecikme değerleriyle öne çıkıyor.");
        }
        if (summary.p95ElapsedTime() > thresholds.maxP95Ms()) {
            return new PerformanceStepInsight(summary.stepName(), PerformanceInsightSeverity.HIGH, PerformanceBottleneckType.LATENCY,
                    "P95", formatNumber(summary.p95ElapsedTime()) + " ms", "<= " + formatNumber(thresholds.maxP95Ms()) + " ms",
                    "Bu adım kullanıcıların önemli bir kısmında yavaşlık yaratabilir.");
        }
        if (summary.averageElapsedTime() > thresholds.maxAverageMs()) {
            return new PerformanceStepInsight(summary.stepName(), PerformanceInsightSeverity.HIGH, PerformanceBottleneckType.LATENCY,
                    "Ortalama", formatNumber(summary.averageElapsedTime()) + " ms", "<= " + formatNumber(thresholds.maxAverageMs()) + " ms",
                    "Bu adım ortalama yanıt süresiyle öne çıkıyor.");
        }
        return new PerformanceStepInsight(summary.stepName(), PerformanceInsightSeverity.INFO, PerformanceBottleneckType.NONE,
                "Analiz", "Öne çıkan adım", null, "Bu adım mevcut analiz tarafından incelenmesi gereken alan olarak işaretlendi.");
    }

    private List<PerformanceRootCauseHint> rootCauseHints(PerformanceEnvironmentMetrics environmentMetrics) {
        List<PerformanceRootCauseHint> hints = new ArrayList<>();
        if (environmentMetrics == null || !environmentMetrics.metricsAvailable()) {
            hints.add(new PerformanceRootCauseHint(
                    PerformanceInsightSeverity.INFO,
                    "OBSERVABILITY",
                    "Ortam metrikleri yok",
                    "Uygulama, veritabanı ve altyapı metrikleri olmadığı için root cause doğrulanamıyor.",
                    "Ortam metriklerini bağlayarak CPU, JVM, DB ve HTTP 5xx sinyallerini test aralığında toplayın."
            ));
            return hints;
        }
        if (environmentMetrics.dbConnectionPoolSize() != null
                && environmentMetrics.dbConnectionPoolSize() > 0
                && environmentMetrics.dbActiveConnectionMax() != null
                && environmentMetrics.dbActiveConnectionMax() >= environmentMetrics.dbConnectionPoolSize() * 0.90) {
            hints.add(new PerformanceRootCauseHint(PerformanceInsightSeverity.HIGH, "DATABASE", "DB pool yüksek",
                    "DB connection pool kullanımı limite yaklaşmış.", "DB pool, slow query ve bağlantı bekleme sürelerini inceleyin."));
        }
        if (environmentMetrics.slowSqlCount() != null && environmentMetrics.slowSqlCount() > 0) {
            hints.add(new PerformanceRootCauseHint(PerformanceInsightSeverity.WARNING, "DATABASE", "Slow SQL",
                    "Test sırasında slow SQL sinyali var.", "Yavaş sorguları problemli step zaman aralığıyla karşılaştırın."));
        }
        if (environmentMetrics.cpuMaxPercent() != null && environmentMetrics.cpuMaxPercent() >= 85) {
            hints.add(new PerformanceRootCauseHint(PerformanceInsightSeverity.WARNING, "INFRASTRUCTURE", "CPU yüksek",
                    "CPU kullanımı yüksek seviyeye çıkmış.", "Test aralığında CPU saturation ve pod kaynak limitlerini kontrol edin."));
        }
        if ((environmentMetrics.jvmHeapMaxPercent() != null && environmentMetrics.jvmHeapMaxPercent() >= 85)
                || (environmentMetrics.gcTimeMs() != null && environmentMetrics.gcTimeMs() > 1000)) {
            hints.add(new PerformanceRootCauseHint(PerformanceInsightSeverity.WARNING, "JVM", "JVM baskısı",
                    "Heap veya GC süresi performansı etkiliyor olabilir.", "GC logları ve heap kullanımını problemli step ile korele edin."));
        }
        if (environmentMetrics.http5xxCount() != null && environmentMetrics.http5xxCount() > 0) {
            hints.add(new PerformanceRootCauseHint(PerformanceInsightSeverity.HIGH, "APPLICATION", "HTTP 5xx",
                    "Test sırasında HTTP 5xx hataları görülmüş.", "Uygulama loglarında 5xx hata nedenlerini adım bazında inceleyin."));
        }
        return hints;
    }

    private boolean hasUnstableStep(List<PerformanceSummary> stepSummaries) {
        return stepSummaries != null
                && stepSummaries.stream()
                .filter(Objects::nonNull)
                .anyMatch(summary -> summary.sampleCount() >= 2
                        && summary.averageElapsedTime() > 0
                        && summary.standardDeviation() >= summary.averageElapsedTime() * 0.50);
    }

    private boolean hasEnvironmentWarnings(PerformanceEnvironmentMetrics environmentMetrics) {
        return environmentMetrics != null
                && environmentMetrics.warnings() != null
                && !environmentMetrics.warnings().isEmpty();
    }

    private boolean hasCriticalRegression(PerformanceComparisonResult baselineComparison) {
        return safeMetrics(baselineComparison).stream()
                .anyMatch(metric -> Boolean.FALSE.equals(metric.improvement())
                        && CRITICAL_REGRESSION_METRICS.contains(metric.metricName()));
    }

    private List<PerformanceComparisonMetric> safeMetrics(PerformanceComparisonResult baselineComparison) {
        if (baselineComparison == null || baselineComparison.metrics() == null) {
            return List.of();
        }
        return baselineComparison.metrics().stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
