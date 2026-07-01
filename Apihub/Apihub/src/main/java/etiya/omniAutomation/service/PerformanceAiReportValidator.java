package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAiActionItem;
import etiya.omniAutomation.business.dto.PerformanceAiManagementReport;
import etiya.omniAutomation.business.dto.PerformanceInsightReport;
import etiya.omniAutomation.business.dto.PerformanceManagementProblemArea;
import etiya.omniAutomation.business.dto.PerformanceManagementReport;
import etiya.omniAutomation.business.dto.PerformanceManagementSlaItem;
import etiya.omniAutomation.business.dto.PerformanceManagementStepAssessment;
import etiya.omniAutomation.business.dto.PerformanceManagementStepStatus;
import etiya.omniAutomation.business.dto.PerformanceMetricInsight;
import etiya.omniAutomation.business.dto.PerformanceReleaseReadiness;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.business.dto.PerformanceStepInsight;
import etiya.omniAutomation.business.dto.PerformanceThresholdResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PerformanceAiReportValidator {

    private static final Set<String> VALID_PRIORITIES = Set.of("P0", "P1", "P2");
    private static final Set<String> BLOCKED_CONTRADICTIONS = Set.of(
            "yayina uygundur",
            "release ready",
            "performans iyi",
            "sorun yok",
            "test basarili"
    );
    private static final Set<String> HEALTHY_PHRASES = Set.of(
            "performans iyi",
            "sorun yok",
            "test basarili",
            "all good",
            "healthy performance"
    );

    public PerformanceAiValidationResult validate(
            PerformanceAiManagementReport aiReport,
            PerformanceManagementReport managementReport,
            PerformanceInsightReport insightReport,
            PerformanceRunSummary runSummary,
            PerformanceThresholdResult thresholdResult
    ) {
        List<String> errors = new ArrayList<>();
        if (aiReport == null) {
            return PerformanceAiValidationResult.invalid(List.of("AI report is required."));
        }
        if (isBlank(aiReport.executiveNarrative())) {
            errors.add("Executive narrative is required.");
        }
        if (isBlank(aiReport.technicalNarrative())) {
            errors.add("Technical narrative is required.");
        }
        if (isBlank(aiReport.releaseReadinessNarrative())) {
            errors.add("Release readiness narrative is required.");
        }

        Set<String> allowedSteps = allowedSteps(managementReport, insightReport);
        Set<String> allowedMetrics = allowedMetrics(managementReport, insightReport);
        for (PerformanceAiActionItem actionItem : safeList(aiReport.recommendedActionPlan())) {
            if (actionItem == null) {
                continue;
            }
            if (!VALID_PRIORITIES.contains(actionItem.priority())) {
                errors.add("Invalid action priority: " + actionItem.priority());
            }
            boolean stepMatches = !isBlank(actionItem.relatedStepName()) && allowedSteps.contains(normalize(actionItem.relatedStepName()));
            boolean metricMatches = !isBlank(actionItem.relatedMetric()) && allowedMetrics.contains(normalizeMetric(actionItem.relatedMetric()));
            if (!stepMatches && !metricMatches) {
                errors.add("Action item is not tied to a deterministic problem: " + nullToEmpty(actionItem.title()));
            }
        }

        String combinedText = combinedText(aiReport);
        if (insightReport != null && insightReport.releaseReadiness() == PerformanceReleaseReadiness.BLOCKED) {
            addPhraseErrors(errors, combinedText, BLOCKED_CONTRADICTIONS, "AI output contradicts blocked release readiness.");
        }
        if (thresholdResult != null && !thresholdResult.passed()) {
            addPhraseErrors(errors, combinedText, HEALTHY_PHRASES, "AI output contradicts failed threshold result.");
        }
        if (insightReport != null) {
            for (PerformanceMetricInsight metricInsight : safeList(insightReport.metricInsights())) {
                if (metricInsight == null || !requiresMetricMention(metricInsight.metric())) {
                    continue;
                }
                if (!mentionsMetric(combinedText, metricInsight.metric())) {
                    errors.add("AI narrative ignores failed metric: " + metricInsight.metric());
                }
            }
        }

        return errors.isEmpty() ? PerformanceAiValidationResult.valid() : PerformanceAiValidationResult.invalid(errors);
    }

    private Set<String> allowedSteps(PerformanceManagementReport managementReport, PerformanceInsightReport insightReport) {
        Set<String> steps = new HashSet<>();
        if (managementReport != null) {
            for (PerformanceManagementStepAssessment assessment : safeList(managementReport.stepAssessments())) {
                if (assessment == null || isBlank(assessment.stepName())) {
                    continue;
                }
                if (assessment.status() == PerformanceManagementStepStatus.NEEDS_IMPROVEMENT
                        || assessment.status() == PerformanceManagementStepStatus.WATCH) {
                    steps.add(normalize(assessment.stepName()));
                }
            }
            for (PerformanceManagementProblemArea problemArea : safeList(managementReport.problemAreas())) {
                if (problemArea != null && !isBlank(problemArea.stepName())) {
                    steps.add(normalize(problemArea.stepName()));
                }
            }
        }
        if (insightReport != null) {
            for (PerformanceStepInsight stepInsight : safeList(insightReport.stepInsights())) {
                if (stepInsight != null && !isBlank(stepInsight.stepName())) {
                    steps.add(normalize(stepInsight.stepName()));
                }
            }
        }
        return steps;
    }

    private Set<String> allowedMetrics(PerformanceManagementReport managementReport, PerformanceInsightReport insightReport) {
        Set<String> metrics = new HashSet<>();
        if (insightReport != null) {
            for (PerformanceMetricInsight metricInsight : safeList(insightReport.metricInsights())) {
                if (metricInsight != null && !isBlank(metricInsight.metric())) {
                    metrics.add(normalizeMetric(metricInsight.metric()));
                }
            }
        }
        if (managementReport != null) {
            for (PerformanceManagementProblemArea problemArea : safeList(managementReport.problemAreas())) {
                if (problemArea != null && !isBlank(problemArea.metric())) {
                    metrics.add(normalizeMetric(problemArea.metric()));
                }
            }
            for (PerformanceManagementSlaItem slaItem : safeList(managementReport.slaSummary())) {
                if (slaItem != null && !slaItem.passed() && !isBlank(slaItem.metric())) {
                    metrics.add(normalizeMetric(slaItem.metric()));
                }
            }
        }
        return metrics;
    }

    private void addPhraseErrors(List<String> errors, String text, Set<String> phrases, String error) {
        for (String phrase : phrases) {
            if (text.contains(phrase)) {
                errors.add(error);
                return;
            }
        }
    }

    private String combinedText(PerformanceAiManagementReport aiReport) {
        StringBuilder text = new StringBuilder()
                .append(nullToEmpty(aiReport.executiveNarrative())).append(' ')
                .append(nullToEmpty(aiReport.technicalNarrative())).append(' ')
                .append(nullToEmpty(aiReport.rootCauseNarrative())).append(' ')
                .append(nullToEmpty(aiReport.releaseReadinessNarrative())).append(' ');
        safeList(aiReport.limitations()).forEach(value -> text.append(nullToEmpty(value)).append(' '));
        safeList(aiReport.recommendedActionPlan()).forEach(action -> {
            if (action != null) {
                text.append(nullToEmpty(action.priority())).append(' ')
                        .append(nullToEmpty(action.title())).append(' ')
                        .append(nullToEmpty(action.description())).append(' ')
                        .append(nullToEmpty(action.relatedStepName())).append(' ')
                        .append(nullToEmpty(action.relatedMetric())).append(' ');
            }
        });
        return normalize(text.toString());
    }

    private boolean requiresMetricMention(String metric) {
        String normalizedMetric = normalizeMetric(metric);
        return normalizedMetric.contains("p95")
                || normalizedMetric.contains("p99")
                || normalizedMetric.contains("hata orani")
                || normalizedMetric.contains("error rate")
                || normalizedMetric.contains("throughput")
                || normalizedMetric.contains("ortalama");
    }

    private boolean mentionsMetric(String combinedText, String metric) {
        for (String alias : metricAliases(metric)) {
            if (combinedText.contains(alias)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> metricAliases(String metric) {
        String normalizedMetric = normalizeMetric(metric);
        Set<String> aliases = new HashSet<>();
        aliases.add(normalizedMetric);
        if (normalizedMetric.contains("p95")) {
            aliases.add("p95");
        }
        if (normalizedMetric.contains("p99")) {
            aliases.add("p99");
        }
        if (normalizedMetric.contains("hata") || normalizedMetric.contains("error")) {
            aliases.add("hata");
            aliases.add("hata orani");
            aliases.add("error rate");
        }
        if (normalizedMetric.contains("throughput")) {
            aliases.add("throughput");
        }
        if (normalizedMetric.contains("ortalama") || normalizedMetric.contains("average")) {
            aliases.add("ortalama");
            aliases.add("average");
        }
        return aliases;
    }

    private String normalizeMetric(String value) {
        return normalize(value).replaceAll("\\s+", " ").trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replace('\u0131', 'i')
                .replace('\u015f', 's')
                .replace('\u011f', 'g')
                .replace('\u00fc', 'u')
                .replace('\u00f6', 'o')
                .replace('\u00e7', 'c')
                .replace('\u0130', 'i')
                .replace('\u015e', 's')
                .replace('\u011e', 'g')
                .replace('\u00dc', 'u')
                .replace('\u00d6', 'o')
                .replace('\u00c7', 'c');
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
