package etiya.omniAutomation.business.dto;

import java.util.Date;
import java.util.List;

public record PerformanceSloScore(
        Integer score,
        PerformanceSloGrade grade,
        PerformanceSloStatus status,
        List<PerformanceSloMetricScore> metricScores,
        List<String> strengths,
        List<String> weaknesses,
        List<String> recommendations,
        Date calculatedAt
) {
}
