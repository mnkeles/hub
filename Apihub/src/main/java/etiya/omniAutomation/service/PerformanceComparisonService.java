package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceComparisonMetric;
import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceRunSummary;
import etiya.omniAutomation.entity.PerfRsltEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PerformanceComparisonService {

    public PerformanceComparisonResult compare(PerfRsltEntity base, PerfRsltEntity target) {
        PerformanceRunSummary baseSummary = base == null ? null : base.getRunSummary();
        PerformanceRunSummary targetSummary = target == null ? null : target.getRunSummary();
        List<PerformanceComparisonMetric> metrics = new ArrayList<>();

        metrics.add(numericMetric("averageResponseTime", average(baseSummary), average(targetSummary), false));
        metrics.add(numericMetric("p90", p90(baseSummary), p90(targetSummary), false));
        metrics.add(numericMetric("p95", p95(baseSummary), p95(targetSummary), false));
        metrics.add(numericMetric("p99", p99(baseSummary), p99(targetSummary), false));
        metrics.add(numericMetric("throughput", throughput(baseSummary), throughput(targetSummary), true));
        metrics.add(numericMetric("errorRate", errorRate(baseSummary), errorRate(targetSummary), false));
        metrics.add(informationalNumericMetric("totalSamples", totalSamples(baseSummary), totalSamples(targetSummary)));
        metrics.add(textMetric("slowestStep", slowestStep(baseSummary), slowestStep(targetSummary)));

        return new PerformanceComparisonResult(
                base == null ? null : base.getPerfRsltId(),
                target == null ? null : target.getPerfRsltId(),
                metrics
        );
    }

    private PerformanceComparisonMetric numericMetric(String metricName, double baseValue, double targetValue, boolean higherIsBetter) {
        double delta = targetValue - baseValue;
        String direction;
        Boolean improvement;

        if (Double.compare(delta, 0) == 0) {
            direction = "UNCHANGED";
            improvement = null;
        } else if ((higherIsBetter && delta > 0) || (!higherIsBetter && delta < 0)) {
            direction = "IMPROVED";
            improvement = true;
        } else {
            direction = "REGRESSED";
            improvement = false;
        }

        return new PerformanceComparisonMetric(metricName, baseValue, targetValue, delta, direction, improvement);
    }

    private PerformanceComparisonMetric informationalNumericMetric(String metricName, long baseValue, long targetValue) {
        long delta = targetValue - baseValue;
        return new PerformanceComparisonMetric(metricName, baseValue, targetValue, delta, delta == 0 ? "UNCHANGED" : "CHANGED", null);
    }

    private PerformanceComparisonMetric textMetric(String metricName, String baseValue, String targetValue) {
        boolean unchanged = Objects.equals(baseValue, targetValue);
        return new PerformanceComparisonMetric(metricName, baseValue, targetValue, null, unchanged ? "UNCHANGED" : "CHANGED", null);
    }

    private double average(PerformanceRunSummary summary) {
        return summary == null ? 0 : summary.averageElapsedTime();
    }

    private double p90(PerformanceRunSummary summary) {
        return summary == null ? 0 : summary.p90ElapsedTime();
    }

    private double p95(PerformanceRunSummary summary) {
        return summary == null ? 0 : summary.p95ElapsedTime();
    }

    private double p99(PerformanceRunSummary summary) {
        return summary == null ? 0 : summary.p99ElapsedTime();
    }

    private double throughput(PerformanceRunSummary summary) {
        return summary == null ? 0 : summary.throughputPerSecond();
    }

    private double errorRate(PerformanceRunSummary summary) {
        return summary == null ? 0 : summary.errorRate();
    }

    private long totalSamples(PerformanceRunSummary summary) {
        return summary == null ? 0 : summary.totalSamples();
    }

    private String slowestStep(PerformanceRunSummary summary) {
        return summary == null ? null : summary.slowestStepName();
    }
}
