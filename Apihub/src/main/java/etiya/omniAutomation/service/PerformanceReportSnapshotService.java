package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAiManagementReport;
import etiya.omniAutomation.business.dto.PerformanceInsightReport;
import etiya.omniAutomation.business.dto.PerformanceManagementReport;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.entity.PerfRsltEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerformanceReportSnapshotService {

    private final PerformanceInsightBuilder performanceInsightBuilder;
    private final PerformanceManagementReportBuilder performanceManagementReportBuilder;
    private final PerformanceAiReportService performanceAiReportService;

    public PerformanceReportSnapshot build(
            PerfRsltEntity result,
            PerformanceThreadGroup threadDetail
    ) {
        PerformanceManagementReport managementReport = performanceManagementReportBuilder.build(
                result.getRunSummary(),
                result.getThresholdResult(),
                result.getAnalysisSummary(),
                result.getErrorAnalysis(),
                result.getEnvironmentMetrics(),
                result.getBaselineComparison(),
                result.getSummary()
        );
        PerformanceInsightReport insightReport = performanceInsightBuilder.build(
                result.getRunSummary(),
                result.getThresholdResult(),
                result.getAnalysisSummary(),
                result.getErrorAnalysis(),
                result.getEnvironmentMetrics(),
                result.getBaselineComparison(),
                result.getSummary(),
                threadDetail
        );
        PerformanceAiManagementReport aiManagementReport = performanceAiReportService.generate(
                managementReport,
                insightReport,
                result.getRunSummary(),
                result.getThresholdResult(),
                result.getAnalysisSummary(),
                result.getErrorAnalysis(),
                result.getEnvironmentMetrics(),
                result.getBaselineComparison(),
                result.getSummary()
        );

        return new PerformanceReportSnapshot(managementReport, insightReport, aiManagementReport);
    }

    public record PerformanceReportSnapshot(
            PerformanceManagementReport managementReport,
            PerformanceInsightReport insightReport,
            PerformanceAiManagementReport aiManagementReport
    ) {
    }
}
