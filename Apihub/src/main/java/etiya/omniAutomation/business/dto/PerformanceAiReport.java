package etiya.omniAutomation.business.dto;

import java.util.Date;
import java.util.List;

public record PerformanceAiReport(
        String executiveSummary,
        String overallStatus,
        String businessImpact,
        List<String> goodPoints,
        List<String> badPoints,
        List<String> risks,
        List<String> recommendedActions,
        String technicalDetails,
        PerformanceAiReportSource source,
        Date generatedAt
) {
}
