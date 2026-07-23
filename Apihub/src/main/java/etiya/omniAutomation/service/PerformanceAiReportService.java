package etiya.omniAutomation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.PerformanceAiReport;
import etiya.omniAutomation.business.dto.PerformanceAiReportSource;
import etiya.omniAutomation.business.dto.PerformanceAnalysisSummary;
import etiya.omniAutomation.business.dto.PerformanceEnvironmentMetrics;
import etiya.omniAutomation.business.dto.PerformanceErrorAnalysis;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceSloScore;
import etiya.omniAutomation.business.dto.PerformanceStepErrorCount;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import etiya.omniAutomation.common.GeneralEnums;
import etiya.omniAutomation.entity.PerfRsltEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PerformanceAiReportService {

    private static final int MAX_WORST_STEPS = 5;
    private static final String SYSTEM_PROMPT = """
            Sen performans testi sonuclarini yoneticiler ve teknik ekipler icin anlasilir rapora donusturen bir asistansin.
            Sadece verilen JSON verisini kaynak olarak kullan. Proje disi tahmin yapma.
            Cevabini yalnizca istenen JSON semasinda ver. Markdown, XML veya ek aciklama yazma.
            """;

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;

    public PerformanceAiReport generateReport(PerfRsltEntity result, PerformanceThreadGroup threadGroup) {
        try {
            String raw = chatClientBuilder.build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(buildUserPrompt(result))
                    .call()
                    .content();
            return parseAiReportResponse(raw);
        } catch (Exception e) {
            return fallbackReport(result, threadGroup, e.getMessage());
        }
    }

    PerformanceAiReport parseAiReportResponse(String raw) throws JsonProcessingException {
        AiReportResponse response = objectMapper.readValue(raw, AiReportResponse.class);
        return new PerformanceAiReport(
                textOrDefault(response.executiveSummary(), "Performans testi raporu olusturuldu."),
                textOrDefault(response.overallStatus(), "UNKNOWN"),
                textOrDefault(response.businessImpact(), "Is etkisi verilen metriklere gore degerlendirilmelidir."),
                safeList(response.goodPoints()),
                safeList(response.badPoints()),
                safeList(response.risks()),
                safeList(response.recommendedActions()),
                textOrDefault(response.technicalDetails(), "-"),
                PerformanceAiReportSource.AI,
                new Date()
        );
    }

    PerformanceAiReport fallbackReport(PerfRsltEntity result, PerformanceThreadGroup threadGroup, String fallbackReason) {
        PerformanceRunSummary runSummary = result == null ? null : result.getRunSummary();
        PerformanceThresholdResult thresholdResult = result == null ? null : result.getThresholdResult();
        PerformanceSloScore sloScore = result == null ? null : result.getSloScore();
        PerformanceAnalysisSummary analysis = result == null ? null : result.getAnalysisSummary();
        PerformanceErrorAnalysis errorAnalysis = result == null ? null : result.getErrorAnalysis();
        String status = overallStatus(result == null ? null : result.getPerfStatus(), thresholdResult);
        String problemStep = firstNonBlank(
                analysis == null ? null : analysis.problemStepName(),
                analysis == null ? null : analysis.slowestStepName(),
                runSummary == null ? null : runSummary.slowestStepName()
        );

        List<String> goodPoints = new ArrayList<>();
        List<String> badPoints = new ArrayList<>();
        List<String> risks = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        if (thresholdResult != null && thresholdResult.passed()) {
            goodPoints.add("Test tanimli threshold degerleri icinde tamamlandi.");
        }
        if (sloScore != null && sloScore.strengths() != null) {
            goodPoints.addAll(safeList(sloScore.strengths()));
        }
        if (runSummary != null) {
            if (runSummary.errorRate() <= 1) {
                goodPoints.add(String.format("Hata orani dusuk gorunuyor: %.2f%%.", runSummary.errorRate()));
            }
            if (runSummary.throughputPerSecond() >= 20) {
                goodPoints.add(String.format("Throughput hedef seviyede veya uzerinde: %.2f req/s.", runSummary.throughputPerSecond()));
            }
        }
        if (goodPoints.isEmpty()) {
            goodPoints.add("Olumlu metrikler sinirli; detayli optimizasyon incelemesi onerilir.");
        }

        if (thresholdResult != null && !thresholdResult.passed()) {
            badPoints.addAll(safeList(thresholdResult.reasons()));
        }
        if (sloScore != null && sloScore.weaknesses() != null) {
            badPoints.addAll(safeList(sloScore.weaknesses()));
        }
        if (errorAnalysis != null && errorAnalysis.totalErrorCount() > 0) {
            badPoints.add(String.format("Toplam %d hatali request olustu.", errorAnalysis.totalErrorCount()));
        }
        if (badPoints.isEmpty()) {
            badPoints.add("Kritik threshold asimi veya hata yogunlugu tespit edilmedi.");
        }

        if (!environmentMetricsAvailable(result == null ? null : result.getEnvironmentMetrics())) {
            risks.add("Ortam metrikleri bulunmadigi icin altyapi kaynakli kok neden analizi sinirlidir.");
        }
        if (fallbackReason != null && !fallbackReason.isBlank()) {
            risks.add("AI rapor uretimi tamamlanamadigi icin kural bazli rapor gosteriliyor.");
        }
        if (status.equals("FAILED")) {
            risks.add("Performans problemi kullanici deneyimini veya servis kararliligini etkileyebilir.");
        }

        if (problemStep != null) {
            actions.add("Oncelikle " + problemStep + " adimi icin servis, sorgu ve bagimli sistem surelerini inceleyin.");
        }
        if (thresholdResult != null && !thresholdResult.passed()) {
            actions.add("Threshold asim nedenlerini P95/P99, ortalama sure, hata orani ve throughput basliklarinda ayri ayri analiz edin.");
        }
        if (sloScore != null && sloScore.recommendations() != null) {
            actions.addAll(safeList(sloScore.recommendations()));
        }
        if (errorAnalysis != null && errorAnalysis.errorsByStep() != null && !errorAnalysis.errorsByStep().isEmpty()) {
            PerformanceStepErrorCount topErrorStep = errorAnalysis.errorsByStep().get(0);
            actions.add(topErrorStep.stepName() + " adimindaki hata detaylarini log ve request bazinda kontrol edin.");
        }
        if (actions.isEmpty()) {
            actions.add("Sonucu onceki baseline testlerle karsilastirip trendi takip edin.");
        }

        return new PerformanceAiReport(
                buildExecutiveSummary(status, thresholdResult, problemStep, sloScore),
                status,
                buildBusinessImpact(status, runSummary),
                goodPoints,
                badPoints,
                risks,
                actions,
                buildTechnicalDetails(runSummary, analysis, errorAnalysis),
                PerformanceAiReportSource.FALLBACK,
                new Date()
        );
    }

    private String buildUserPrompt(PerfRsltEntity result) throws JsonProcessingException {
        AiReportInput input = new AiReportInput(
                result == null ? null : result.getPerfStatus(),
                result == null ? null : result.getRunSummary(),
                result == null ? null : result.getThresholdResult(),
                result == null ? null : result.getAnalysisSummary(),
                result == null ? null : result.getErrorAnalysis(),
                result == null ? null : result.getEnvironmentMetrics(),
                result == null ? null : result.getBaselineComparison(),
                result == null ? null : result.getSloScore(),
                worstSteps(result == null ? null : result.getSummary())
        );
        return """
                Asagidaki performans testi verisini iki katmanli rapora donustur.
                JSON semasi:
                {
                  "executiveSummary": "string",
                  "overallStatus": "PASSED|FAILED|STOPPED|ERROR",
                  "businessImpact": "string",
                  "goodPoints": ["string"],
                  "badPoints": ["string"],
                  "risks": ["string"],
                  "recommendedActions": ["string"],
                  "technicalDetails": "string"
                }

                Veri:
                """ + objectMapper.writeValueAsString(input);
    }

    private List<PerformanceSummary> worstSteps(List<PerformanceSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return List.of();
        }
        return summaries.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparingLong(PerformanceSummary::failureCount)
                        .thenComparingDouble(PerformanceSummary::p99ElapsedTime)
                        .thenComparingDouble(PerformanceSummary::p95ElapsedTime)
                        .thenComparingDouble(PerformanceSummary::averageElapsedTime)
                        .reversed())
                .limit(MAX_WORST_STEPS)
                .toList();
    }

    private String overallStatus(GeneralEnums.PerformanceStatus status, PerformanceThresholdResult thresholdResult) {
        if (status == GeneralEnums.PerformanceStatus.COMPLETED_PASSED || (thresholdResult != null && thresholdResult.passed())) {
            return "PASSED";
        }
        if (status == GeneralEnums.PerformanceStatus.COMPLETED_FAILED || (thresholdResult != null && !thresholdResult.passed())) {
            return "FAILED";
        }
        if (status == GeneralEnums.PerformanceStatus.STOPPED) {
            return "STOPPED";
        }
        if (status == GeneralEnums.PerformanceStatus.ERROR) {
            return "ERROR";
        }
        return status == null ? "UNKNOWN" : status.name();
    }

    private String buildExecutiveSummary(String status, PerformanceThresholdResult thresholdResult, String problemStep, PerformanceSloScore sloScore) {
        String prefix = sloScore == null
                ? ""
                : "SLO skoru " + sloScore.score() + "/100 (" + sloScore.grade() + "). ";
        if ("PASSED".equals(status)) {
            return prefix + "Performans testi genel olarak kabul edilebilir seviyede tamamlandi.";
        }
        if ("FAILED".equals(status)) {
            String reason = thresholdResult == null || thresholdResult.reasons() == null || thresholdResult.reasons().isEmpty()
                    ? "threshold asimi tespit edildi"
                    : thresholdResult.reasons().get(0);
            return prefix + "Performans testi basarisiz degerlendirildi. Ana neden: " + reason
                    + (problemStep == null ? "." : ". Oncelikli adim: " + problemStep + ".");
        }
        if ("STOPPED".equals(status)) {
            return prefix + "Performans testi tamamlanmadan durduruldu; sonuc trend ve kapasite yorumu icin sinirli kullanilmalidir.";
        }
        if ("ERROR".equals(status)) {
            return prefix + "Performans testi teknik hata nedeniyle tamamlanamadi; test kosumu ve altyapi loglari kontrol edilmelidir.";
        }
        return prefix + "Performans testi icin kural bazli rapor olusturuldu.";
    }

    private String buildBusinessImpact(String status, PerformanceRunSummary runSummary) {
        if ("PASSED".equals(status)) {
            return "Mevcut metrikler is akisi icin kabul edilebilir performansa isaret ediyor.";
        }
        if ("FAILED".equals(status)) {
            return "Yuksek gecikme, hata orani veya dusuk throughput kullanici deneyiminde yavaslama ve operasyonel risk olusturabilir.";
        }
        if (runSummary == null || runSummary.totalSamples() == 0) {
            return "Yeterli tamamlanmis sample olmadigi icin is etkisi net hesaplanamadi.";
        }
        return "Test sonucu sinirli yorumlanmali; karar icin tekrar kosum veya baseline karsilastirmasi onerilir.";
    }

    private String buildTechnicalDetails(PerformanceRunSummary runSummary, PerformanceAnalysisSummary analysis, PerformanceErrorAnalysis errorAnalysis) {
        if (runSummary == null) {
            return "Run summary bulunamadigi icin teknik detay sinirli.";
        }
        return String.format(
                "Total sample: %d, hata orani: %.2f%%, throughput: %.2f req/s, ortalama: %.0f ms, P95: %.0f ms, P99: %.0f ms, en yavas adim: %s, en yuksek P95: %s, en yuksek P99: %s, toplam hata: %d.",
                runSummary.totalSamples(),
                runSummary.errorRate(),
                runSummary.throughputPerSecond(),
                runSummary.averageElapsedTime(),
                runSummary.p95ElapsedTime(),
                runSummary.p99ElapsedTime(),
                valueOrDash(runSummary.slowestStepName()),
                valueOrDash(analysis == null ? null : analysis.highestP95StepName()),
                valueOrDash(analysis == null ? null : analysis.highestP99StepName()),
                errorAnalysis == null ? 0 : errorAnalysis.totalErrorCount()
        );
    }

    private boolean environmentMetricsAvailable(PerformanceEnvironmentMetrics metrics) {
        return metrics != null && metrics.metricsAvailable();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static String textOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static List<String> safeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .toList();
    }

    private record AiReportResponse(
            String executiveSummary,
            String overallStatus,
            String businessImpact,
            List<String> goodPoints,
            List<String> badPoints,
            List<String> risks,
            List<String> recommendedActions,
            String technicalDetails
    ) {
    }

    private record AiReportInput(
            GeneralEnums.PerformanceStatus status,
            PerformanceRunSummary runSummary,
            PerformanceThresholdResult thresholdResult,
            PerformanceAnalysisSummary analysisSummary,
            PerformanceErrorAnalysis errorAnalysis,
            PerformanceEnvironmentMetrics environmentMetrics,
            Object baselineComparison,
            PerformanceSloScore sloScore,
            List<PerformanceSummary> worstStepSummaries
    ) {
    }
}
