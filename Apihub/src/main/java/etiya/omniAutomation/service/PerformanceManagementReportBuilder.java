package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAnalysisSummary;
import etiya.omniAutomation.business.dto.PerformanceComparisonMetric;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceManagementProblemArea;
import etiya.omniAutomation.business.dto.PerformanceManagementReport;
import etiya.omniAutomation.business.dto.PerformanceManagementRiskLevel;
import etiya.omniAutomation.business.dto.PerformanceManagementSlaItem;
import etiya.omniAutomation.business.dto.PerformanceManagementStepAssessment;
import etiya.omniAutomation.business.dto.PerformanceManagementStepStatus;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThresholdConfig;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.common.GeneralEnums;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class PerformanceManagementReportBuilder {

    private static final String BASELINE_UNAVAILABLE =
            "Baseline karsilastirmasi bulunmuyor. Performans trendini gormek icin bir baseline kosumu secin.";

    private enum StepReason {
        ERROR_RATE,
        P99,
        P95,
        AVERAGE,
        INSTABILITY,
        THROUGHPUT,
        ANALYSIS_HIGHLIGHT,
        ZERO_SAMPLES,
        GOOD
    }

    public PerformanceManagementReport build(
            PerformanceRunSummary runSummary,
            PerformanceThresholdResult thresholdResult,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceErrorAnalysis errorAnalysis,
            PerformanceEnvironmentMetrics environmentMetrics,
            PerformanceComparisonResult baselineComparison,
            List<PerformanceSummary> stepSummaries
    ) {
        String overallStatus = overallStatus(runSummary, thresholdResult, errorAnalysis);
        PerformanceManagementRiskLevel riskLevel = riskLevel(runSummary, thresholdResult, environmentMetrics, baselineComparison);
        List<PerformanceManagementStepAssessment> stepAssessments = stepAssessments(analysisSummary, stepSummaries, thresholdResult);
        String stepAssessmentSummary = stepAssessmentSummary(stepAssessments);
        List<PerformanceManagementSlaItem> slaSummary = slaSummary(runSummary, thresholdResult);
        List<PerformanceManagementProblemArea> problemAreas = problemAreas(analysisSummary, stepSummaries);
        String trendSummary = trendSummary(baselineComparison);
        List<String> recommendedActions = recommendedActions(runSummary, thresholdResult, errorAnalysis, environmentMetrics, baselineComparison);
        String executiveSummary = executiveSummary(overallStatus, riskLevel, analysisSummary, thresholdResult);
        String detailExplanation = detailExplanation(overallStatus, riskLevel, analysisSummary, baselineComparison);

        return new PerformanceManagementReport(
                overallStatus,
                riskLevel,
                stepAssessmentSummary,
                stepAssessments,
                executiveSummary,
                slaSummary,
                problemAreas,
                trendSummary,
                recommendedActions,
                detailExplanation
        );
    }

    private List<PerformanceManagementStepAssessment> stepAssessments(
            PerformanceAnalysisSummary analysisSummary,
            List<PerformanceSummary> stepSummaries,
            PerformanceThresholdResult thresholdResult
    ) {
        if (stepSummaries == null || stepSummaries.isEmpty()) {
            return List.of();
        }
        PerformanceThresholdConfig thresholds = thresholds(thresholdResult);
        return stepSummaries.stream()
                .filter(Objects::nonNull)
                .map(summary -> stepAssessment(summary, analysisSummary, thresholds))
                .sorted(stepAssessmentComparator())
                .toList();
    }

    private PerformanceManagementStepAssessment stepAssessment(
            PerformanceSummary summary,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceThresholdConfig thresholds
    ) {
        PerformanceManagementStepStatus status = stepStatus(summary, analysisSummary, thresholds);
        StepReason reason = stepReason(summary, analysisSummary, thresholds);
        return new PerformanceManagementStepAssessment(
                summary.stepName(),
                status,
                stepPriority(summary, status, thresholds),
                reasonText(reason, summary, thresholds),
                evidence(reason, summary, thresholds),
                impact(reason),
                recommendation(reason),
                summary.sampleCount(),
                summary.successCount(),
                summary.failureCount(),
                summary.errorRate(),
                summary.averageElapsedTime(),
                summary.p90ElapsedTime(),
                summary.p95ElapsedTime(),
                summary.p99ElapsedTime(),
                summary.throughputPerSecond(),
                summary.lastError()
        );
    }

    private PerformanceManagementStepStatus stepStatus(
            PerformanceSummary summary,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceThresholdConfig thresholds
    ) {
        if (needsImprovement(summary, analysisSummary, thresholds)) {
            return PerformanceManagementStepStatus.NEEDS_IMPROVEMENT;
        }
        if (shouldWatch(summary, thresholds)) {
            return PerformanceManagementStepStatus.WATCH;
        }
        return PerformanceManagementStepStatus.GOOD;
    }

    private boolean needsImprovement(
            PerformanceSummary summary,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceThresholdConfig thresholds
    ) {
        return summary.errorRate() > thresholds.maxErrorRatePercent()
                || summary.averageElapsedTime() > thresholds.maxAverageMs()
                || summary.p95ElapsedTime() > thresholds.maxP95Ms()
                || summary.p99ElapsedTime() > thresholds.maxP99Ms()
                || isAnalysisHighlightedStep(summary.stepName(), analysisSummary);
    }

    private boolean shouldWatch(PerformanceSummary summary, PerformanceThresholdConfig thresholds) {
        return summary.sampleCount() <= 0
                || summary.errorRate() >= thresholds.maxErrorRatePercent() * 0.70
                || summary.averageElapsedTime() >= thresholds.maxAverageMs() * 0.80
                || summary.p95ElapsedTime() >= thresholds.maxP95Ms() * 0.80
                || summary.p99ElapsedTime() >= thresholds.maxP99Ms() * 0.80
                || summary.failureCount() > 0
                || (summary.standardDeviation() >= summary.averageElapsedTime() * 0.50 && summary.sampleCount() >= 2)
                || (summary.throughputPerSecond() < thresholds.minThroughputPerSecond() && summary.sampleCount() > 0);
    }

    private boolean isAnalysisHighlightedStep(String stepName, PerformanceAnalysisSummary analysisSummary) {
        if (stepName == null || analysisSummary == null) {
            return false;
        }
        return Objects.equals(stepName, analysisSummary.problemStepName())
                || Objects.equals(stepName, analysisSummary.slowestStepName())
                || Objects.equals(stepName, analysisSummary.highestErrorStepName());
    }

    private StepReason stepReason(
            PerformanceSummary summary,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceThresholdConfig thresholds
    ) {
        if (summary.errorRate() > thresholds.maxErrorRatePercent()
                || summary.errorRate() >= thresholds.maxErrorRatePercent() * 0.70) {
            return StepReason.ERROR_RATE;
        }
        if (summary.p99ElapsedTime() > thresholds.maxP99Ms()
                || summary.p99ElapsedTime() >= thresholds.maxP99Ms() * 0.80) {
            return StepReason.P99;
        }
        if (summary.p95ElapsedTime() > thresholds.maxP95Ms()
                || summary.p95ElapsedTime() >= thresholds.maxP95Ms() * 0.80) {
            return StepReason.P95;
        }
        if (summary.averageElapsedTime() > thresholds.maxAverageMs()
                || summary.averageElapsedTime() >= thresholds.maxAverageMs() * 0.80) {
            return StepReason.AVERAGE;
        }
        if (summary.standardDeviation() >= summary.averageElapsedTime() * 0.50 && summary.sampleCount() >= 2) {
            return StepReason.INSTABILITY;
        }
        if (summary.throughputPerSecond() < thresholds.minThroughputPerSecond() && summary.sampleCount() > 0) {
            return StepReason.THROUGHPUT;
        }
        if (isAnalysisHighlightedStep(summary.stepName(), analysisSummary)) {
            return StepReason.ANALYSIS_HIGHLIGHT;
        }
        if (summary.sampleCount() <= 0) {
            return StepReason.ZERO_SAMPLES;
        }
        return StepReason.GOOD;
    }

    private PerformanceManagementRiskLevel stepPriority(
            PerformanceSummary summary,
            PerformanceManagementStepStatus status,
            PerformanceThresholdConfig thresholds
    ) {
        if (summary.errorRate() >= thresholds.maxErrorRatePercent() * 2
                || summary.p99ElapsedTime() >= thresholds.maxP99Ms() * 2) {
            return PerformanceManagementRiskLevel.CRITICAL;
        }
        return switch (status) {
            case NEEDS_IMPROVEMENT -> PerformanceManagementRiskLevel.HIGH;
            case WATCH -> PerformanceManagementRiskLevel.MEDIUM;
            case GOOD -> PerformanceManagementRiskLevel.LOW;
        };
    }

    private Comparator<PerformanceManagementStepAssessment> stepAssessmentComparator() {
        return Comparator
                .comparingInt((PerformanceManagementStepAssessment assessment) -> stepStatusOrder(assessment.status()))
                .thenComparingInt(assessment -> riskOrder(assessment.priority()))
                .thenComparing(Comparator.comparingDouble(PerformanceManagementStepAssessment::errorRate).reversed())
                .thenComparing(Comparator.comparingDouble(PerformanceManagementStepAssessment::p99Ms).reversed())
                .thenComparing(Comparator.comparingDouble(PerformanceManagementStepAssessment::p95Ms).reversed())
                .thenComparing(PerformanceManagementStepAssessment::stepName, Comparator.nullsLast(String::compareTo));
    }

    private int stepStatusOrder(PerformanceManagementStepStatus status) {
        if (status == PerformanceManagementStepStatus.NEEDS_IMPROVEMENT) {
            return 0;
        }
        if (status == PerformanceManagementStepStatus.WATCH) {
            return 1;
        }
        return 2;
    }

    private int riskOrder(PerformanceManagementRiskLevel riskLevel) {
        if (riskLevel == PerformanceManagementRiskLevel.CRITICAL) {
            return 0;
        }
        if (riskLevel == PerformanceManagementRiskLevel.HIGH) {
            return 1;
        }
        if (riskLevel == PerformanceManagementRiskLevel.MEDIUM) {
            return 2;
        }
        return 3;
    }

    private String stepAssessmentSummary(List<PerformanceManagementStepAssessment> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            return "Bu test için adım bazlı analiz üretilemedi.";
        }
        long needsImprovement = countByStatus(assessments, PerformanceManagementStepStatus.NEEDS_IMPROVEMENT);
        long watch = countByStatus(assessments, PerformanceManagementStepStatus.WATCH);
        long good = countByStatus(assessments, PerformanceManagementStepStatus.GOOD);
        if (needsImprovement == 0 && watch == 0) {
            return String.format(Locale.US,
                    "%d adımın tamamı iyi durumda. Şu anda iyileştirme gerektiren adım bulunmuyor.",
                    assessments.size());
        }
        return String.format(Locale.US,
                "%d adımdan %d adım iyileştirilmeli, %d adım izlenmeli, %d adım iyi durumda.",
                assessments.size(),
                needsImprovement,
                watch,
                good);
    }

    private long countByStatus(List<PerformanceManagementStepAssessment> assessments, PerformanceManagementStepStatus status) {
        return assessments.stream()
                .filter(assessment -> assessment.status() == status)
                .count();
    }

    private String reasonText(StepReason reason, PerformanceSummary summary, PerformanceThresholdConfig thresholds) {
        return switch (reason) {
            case ERROR_RATE -> summary.errorRate() > thresholds.maxErrorRatePercent()
                    ? "Hata oranı hedefin üzerinde."
                    : "Hata oranı hedefe yakın.";
            case P99 -> summary.p99ElapsedTime() > thresholds.maxP99Ms()
                    ? "P99 hedefin üzerinde."
                    : "P99 hedefe yakın.";
            case P95 -> summary.p95ElapsedTime() > thresholds.maxP95Ms()
                    ? "P95 hedefin üzerinde."
                    : "P95 hedefe yakın.";
            case AVERAGE -> summary.averageElapsedTime() > thresholds.maxAverageMs()
                    ? "Ortalama yanıt süresi hedefin üzerinde."
                    : "Ortalama yanıt süresi hedefe yakın.";
            case INSTABILITY -> "Yanıt süreleri dalgalı.";
            case THROUGHPUT -> "Throughput beklenen seviyenin altında.";
            case ANALYSIS_HIGHLIGHT -> "Mevcut analiz bu adımı öncelikli alan olarak işaretledi.";
            case ZERO_SAMPLES -> "Bu adımda anlamlı trafik oluşmadı.";
            case GOOD -> "Bu adım hedeflerin içinde çalışıyor.";
        };
    }

    private String evidence(StepReason reason, PerformanceSummary summary, PerformanceThresholdConfig thresholds) {
        return switch (reason) {
            case ERROR_RATE -> String.format(Locale.US,
                    "Hata oranı %.2f%%, hedef <= %.2f%%",
                    summary.errorRate(),
                    thresholds.maxErrorRatePercent());
            case P99 -> String.format(Locale.US,
                    "P99 %.0f ms, hedef <= %.0f ms",
                    summary.p99ElapsedTime(),
                    thresholds.maxP99Ms());
            case P95 -> String.format(Locale.US,
                    "P95 %.0f ms, hedef <= %.0f ms",
                    summary.p95ElapsedTime(),
                    thresholds.maxP95Ms());
            case AVERAGE -> String.format(Locale.US,
                    "Ortalama %.0f ms, hedef <= %.0f ms",
                    summary.averageElapsedTime(),
                    thresholds.maxAverageMs());
            case INSTABILITY -> String.format(Locale.US,
                    "Standart sapma %.0f ms, ortalama %.0f ms",
                    summary.standardDeviation(),
                    summary.averageElapsedTime());
            case THROUGHPUT -> String.format(Locale.US,
                    "Throughput %.2f req/s, hedef >= %.2f req/s",
                    summary.throughputPerSecond(),
                    thresholds.minThroughputPerSecond());
            case ZERO_SAMPLES -> "Toplam istek 0";
            case ANALYSIS_HIGHLIGHT, GOOD -> String.format(Locale.US,
                    "Hata oranı %.2f%%, P95 %.0f ms",
                    summary.errorRate(),
                    summary.p95ElapsedTime());
        };
    }

    private String impact(StepReason reason) {
        return switch (reason) {
            case ERROR_RATE -> "Başarısız istekler kullanıcı akışını kesebilir.";
            case P99, P95, AVERAGE -> "Kullanıcıların bir kısmı bu adımda yavaşlık yaşayabilir.";
            case INSTABILITY -> "Adım bazı çalışmalarda hızlı, bazı çalışmalarda yavaş yanıt veriyor.";
            case THROUGHPUT -> "Adım beklenen yükü karşılamakta zorlanabilir.";
            case ZERO_SAMPLES -> "Bu adım için performans yorumu yapmak için yeterli veri yok.";
            case ANALYSIS_HIGHLIGHT -> "Bu adım genel performans sonucunu etkileyen öncelikli alan olabilir.";
            case GOOD -> "Bu adım mevcut koşullarda hedefleri karşılıyor.";
        };
    }

    private String recommendation(StepReason reason) {
        return switch (reason) {
            case ERROR_RATE -> "Hata alan istekleri ve son hata mesajını adım bazında inceleyin.";
            case P99, P95, AVERAGE -> "Bu adımın servis, veritabanı ve downstream bağımlılık sürelerini inceleyin.";
            case INSTABILITY -> "Dalgalanmayı artıran kaynak kullanımı veya dış servis beklemelerini kontrol edin.";
            case THROUGHPUT -> "Bu adım için kapasite, bağlantı havuzu ve throttling ayarlarını kontrol edin.";
            case ZERO_SAMPLES -> "Test senaryosunda bu adımın gerçekten çalıştığını doğrulayın.";
            case ANALYSIS_HIGHLIGHT -> "Bu adımı ilgili metrik detaylarıyla birlikte öncelikli olarak inceleyin.";
            case GOOD -> "Ek aksiyon gerekmiyor; sonraki koşumlarda izlemeye devam edin.";
        };
    }

    private String overallStatus(PerformanceRunSummary runSummary, PerformanceThresholdResult thresholdResult, PerformanceErrorAnalysis errorAnalysis) {
        GeneralEnums.PerformanceStatus status = runSummary == null ? null : runSummary.status();
        if (status == GeneralEnums.PerformanceStatus.ERROR) {
            return "Hata";
        }
        if (status == GeneralEnums.PerformanceStatus.STOPPED) {
            return "Durduruldu";
        }
        if (status == GeneralEnums.PerformanceStatus.RUNNING || status == GeneralEnums.PerformanceStatus.STOPPING) {
            return "Calisiyor";
        }
        if (thresholdResult != null && thresholdResult.passed()) {
            return "Basarili";
        }
        if (thresholdResult != null && !thresholdResult.passed()) {
            return "Basarisiz";
        }
        if (status == GeneralEnums.PerformanceStatus.COMPLETED_PASSED) {
            return "Basarili";
        }
        if (status == GeneralEnums.PerformanceStatus.COMPLETED_FAILED || status == GeneralEnums.PerformanceStatus.FAILED) {
            return "Basarisiz";
        }
        if (errorAnalysis != null && errorAnalysis.totalErrorCount() > 0) {
            return "Basarisiz";
        }
        if (runSummary != null && runSummary.failedSamples() > 0) {
            return "Basarisiz";
        }
        if (status == GeneralEnums.PerformanceStatus.COMPLETED) {
            return "Basarili";
        }
        return "Bilinmiyor";
    }

    private PerformanceManagementRiskLevel riskLevel(
            PerformanceRunSummary runSummary,
            PerformanceThresholdResult thresholdResult,
            PerformanceEnvironmentMetrics environmentMetrics,
            PerformanceComparisonResult baselineComparison
    ) {
        PerformanceThresholdConfig thresholds = thresholds(thresholdResult);
        GeneralEnums.PerformanceStatus status = runSummary == null ? null : runSummary.status();
        double errorRate = runSummary == null ? 0 : runSummary.errorRate();
        double p95 = runSummary == null ? 0 : runSummary.p95ElapsedTime();
        double p99 = runSummary == null ? 0 : runSummary.p99ElapsedTime();

        if (status == GeneralEnums.PerformanceStatus.ERROR || errorRate >= 10 || p99 >= thresholds.maxP99Ms() * 2) {
            return PerformanceManagementRiskLevel.CRITICAL;
        }
        if ((thresholdResult != null && !thresholdResult.passed()) || errorRate >= 1 || p95 > thresholds.maxP95Ms()) {
            return PerformanceManagementRiskLevel.HIGH;
        }
        if (hasRegression(baselineComparison) || hasEnvironmentWarnings(environmentMetrics)) {
            return PerformanceManagementRiskLevel.MEDIUM;
        }
        return PerformanceManagementRiskLevel.LOW;
    }

    private List<PerformanceManagementSlaItem> slaSummary(PerformanceRunSummary runSummary, PerformanceThresholdResult thresholdResult) {
        if (runSummary == null) {
            return List.of();
        }
        PerformanceThresholdConfig thresholds = thresholds(thresholdResult);
        return List.of(
                new PerformanceManagementSlaItem(
                        "Hata orani",
                        runSummary.errorRate() <= thresholds.maxErrorRatePercent(),
                        "<= " + formatPercent(thresholds.maxErrorRatePercent()),
                        formatPercent(runSummary.errorRate()),
                        "Hata orani test sirasinda isteklerin ne kadarinin basarisiz oldugunu gosterir."
                ),
                new PerformanceManagementSlaItem(
                        "Ortalama yanit suresi",
                        runSummary.averageElapsedTime() <= thresholds.maxAverageMs(),
                        "<= " + formatMs(thresholds.maxAverageMs()),
                        formatMs(runSummary.averageElapsedTime()),
                        "Ortalama yanit suresi tum istekler icin tipik tamamlanma suresini gosterir."
                ),
                new PerformanceManagementSlaItem(
                        "P95",
                        runSummary.p95ElapsedTime() <= thresholds.maxP95Ms(),
                        "<= " + formatMs(thresholds.maxP95Ms()),
                        formatMs(runSummary.p95ElapsedTime()),
                        "P95 en yavas yuzde 5'lik istek grubunun yasadigi yanit suresini gosterir."
                ),
                new PerformanceManagementSlaItem(
                        "P99",
                        runSummary.p99ElapsedTime() <= thresholds.maxP99Ms(),
                        "<= " + formatMs(thresholds.maxP99Ms()),
                        formatMs(runSummary.p99ElapsedTime()),
                        "P99 az sayida kullaniciyi etkileyebilecek uc gecikme durumlarini gosterir."
                ),
                new PerformanceManagementSlaItem(
                        "Throughput",
                        runSummary.throughputPerSecond() >= thresholds.minThroughputPerSecond(),
                        ">= " + formatThroughput(thresholds.minThroughputPerSecond()),
                        formatThroughput(runSummary.throughputPerSecond()),
                        "Throughput akis tarafindan saniyede kac istegin karsilandigini gosterir."
                )
        );
    }

    private List<PerformanceManagementProblemArea> problemAreas(PerformanceAnalysisSummary analysisSummary, List<PerformanceSummary> stepSummaries) {
        if (analysisSummary == null) {
            return List.of();
        }
        List<PerformanceManagementProblemArea> areas = new ArrayList<>();
        Set<String> addedSteps = new LinkedHashSet<>();
        addProblemArea(areas, addedSteps, "Ana problemli adim", analysisSummary.problemStepName(), "P95/P99", stepSummaries,
                "Optimizasyon icin ilk incelenmesi gereken adimdir.");
        addProblemArea(areas, addedSteps, "En yavas adim", analysisSummary.slowestStepName(), "Ortalama", stepSummaries,
                "En yuksek ortalama yanit suresine sahip adimdir.");
        addProblemArea(areas, addedSteps, "En cok hata alan adim", analysisSummary.highestErrorStepName(), "Hata orani", stepSummaries,
                "Basarisiz isteklere en fazla katkida bulunan adimdir.");
        return areas;
    }

    private void addProblemArea(
            List<PerformanceManagementProblemArea> areas,
            Set<String> addedSteps,
            String title,
            String stepName,
            String metric,
            List<PerformanceSummary> stepSummaries,
            String impact
    ) {
        if (stepName == null || stepName.isBlank() || addedSteps.contains(stepName)) {
            return;
        }
        PerformanceSummary summary = findSummary(stepSummaries, stepName).orElse(null);
        areas.add(new PerformanceManagementProblemArea(
                title,
                stepName,
                metric,
                problemValue(metric, summary),
                impact
        ));
        addedSteps.add(stepName);
    }

    private String problemValue(String metric, PerformanceSummary summary) {
        if (summary == null) {
            return null;
        }
        return switch (metric) {
            case "Ortalama" -> formatMs(summary.averageElapsedTime());
            case "Hata orani" -> formatPercent(summary.errorRate());
            default -> "P95 " + formatMs(summary.p95ElapsedTime()) + ", P99 " + formatMs(summary.p99ElapsedTime());
        };
    }

    private Optional<PerformanceSummary> findSummary(List<PerformanceSummary> stepSummaries, String stepName) {
        if (stepSummaries == null || stepName == null) {
            return Optional.empty();
        }
        return stepSummaries.stream()
                .filter(Objects::nonNull)
                .filter(summary -> stepName.equals(summary.stepName()))
                .findFirst();
    }

    private String trendSummary(PerformanceComparisonResult baselineComparison) {
        if (baselineComparison == null || baselineComparison.metrics() == null || baselineComparison.metrics().isEmpty()) {
            return BASELINE_UNAVAILABLE;
        }
        long improved = baselineComparison.metrics().stream()
                .filter(metric -> Boolean.TRUE.equals(metric.improvement()))
                .count();
        long regressed = baselineComparison.metrics().stream()
                .filter(metric -> Boolean.FALSE.equals(metric.improvement()))
                .count();
        long changed = baselineComparison.metrics().stream()
                .filter(metric -> metric.improvement() == null)
                .count();
        return String.format(Locale.US,
                "Baseline ile karsilastirma: %d metrik iyilesti, %d metrik kotulesti, %d bilgi amacli metrik degisti.",
                improved,
                regressed,
                changed);
    }

    private List<String> recommendedActions(
            PerformanceRunSummary runSummary,
            PerformanceThresholdResult thresholdResult,
            PerformanceErrorAnalysis errorAnalysis,
            PerformanceEnvironmentMetrics environmentMetrics,
            PerformanceComparisonResult baselineComparison
    ) {
        List<String> actions = new ArrayList<>();
        if (thresholdResult != null && !thresholdResult.passed()) {
            actions.add("Threshold basarisizlik nedenlerini inceleyin ve listelenen yavas ya da hatali adimi onceliklendirin.");
        }
        if (percentileThresholdFailed(runSummary, thresholdResult)) {
            actions.add("Yuku artirmadan once problemli adimdaki yuksek percentile gecikmesini inceleyin.");
        }
        if (errorAnalysis != null && errorAnalysis.totalErrorCount() > 0) {
            actions.add("Testi tekrar calistirmadan once hatali istekleri inceleyin ve hatalari adim bazinda gruplayin.");
        }
        if (environmentMetrics == null || Boolean.FALSE.equals(environmentMetrics.metricsAvailable())) {
            actions.add("Uygulama, veritabani ve altyapi kaynaklarini ayirmak icin ortam metriklerini baglayin.");
        }
        if (hasRegression(baselineComparison)) {
            actions.add("Surumu onaylamadan once kotulesen metrikleri secili baseline ile karsilastirin.");
        }
        if (actions.isEmpty()) {
            actions.add("Bu sonucu baseline adayi olarak saklayin ve sonraki kosumda regresyon olup olmadigini izleyin.");
        }
        return actions;
    }

    private boolean percentileThresholdFailed(PerformanceRunSummary runSummary, PerformanceThresholdResult thresholdResult) {
        if (runSummary == null) {
            return false;
        }
        PerformanceThresholdConfig thresholds = thresholds(thresholdResult);
        return runSummary.p95ElapsedTime() > thresholds.maxP95Ms()
                || runSummary.p99ElapsedTime() > thresholds.maxP99Ms();
    }

    private String executiveSummary(
            String overallStatus,
            PerformanceManagementRiskLevel riskLevel,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceThresholdResult thresholdResult
    ) {
        String problemStep = analysisSummary == null ? null : analysisSummary.problemStepName();
        if ("Basarili".equals(overallStatus)) {
            return "Test tanimli performans threshold degerlerini gecti. Risk seviyesi: " + riskLevel + ".";
        }
        if (thresholdResult != null && !thresholdResult.passed()) {
            String stepText = problemStep == null ? "one cikan akis adimlari" : problemStep;
            return "Test performans acisindan basarisiz gorunuyor. Risk seviyesi: " + riskLevel
                    + ". Oncelikli incelenecek alan: " + stepText + ".";
        }
        return "Test " + overallStatus + " durumu ile tamamlandi. Risk seviyesi: " + riskLevel + ".";
    }

    private String detailExplanation(
            String overallStatus,
            PerformanceManagementRiskLevel riskLevel,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceComparisonResult baselineComparison
    ) {
        String problemStep = analysisSummary == null || analysisSummary.problemStepName() == null
                ? "tek bir problemli adim tespit edilmedi"
                : "ana problemli adim " + analysisSummary.problemStepName();
        String baselineText = baselineComparison == null || baselineComparison.metrics() == null || baselineComparison.metrics().isEmpty()
                ? "Baseline karsilastirmasi bulunmuyor."
                : "Baseline karsilastirmasi mevcut ve trend ozetine dahil edildi.";
        return "Genel sonuc " + overallStatus + ", risk seviyesi " + riskLevel + "; " + problemStep + ". " + baselineText;
    }

    private PerformanceThresholdConfig thresholds(PerformanceThresholdResult thresholdResult) {
        return thresholdResult == null || thresholdResult.thresholds() == null
                ? PerformanceThresholdConfig.defaults()
                : thresholdResult.thresholds();
    }

    private boolean hasRegression(PerformanceComparisonResult baselineComparison) {
        return baselineComparison != null
                && baselineComparison.metrics() != null
                && baselineComparison.metrics().stream()
                .filter(Objects::nonNull)
                .map(PerformanceComparisonMetric::improvement)
                .anyMatch(Boolean.FALSE::equals);
    }

    private boolean hasEnvironmentWarnings(PerformanceEnvironmentMetrics environmentMetrics) {
        return environmentMetrics != null
                && environmentMetrics.warnings() != null
                && !environmentMetrics.warnings().isEmpty();
    }

    private String formatMs(double value) {
        return String.format(Locale.US, "%.0f ms", value);
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.2f%%", value);
    }

    private String formatThroughput(double value) {
        return String.format(Locale.US, "%.2f req/s", value);
    }
}
