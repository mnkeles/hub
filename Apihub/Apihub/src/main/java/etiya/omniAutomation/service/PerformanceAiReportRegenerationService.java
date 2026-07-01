package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAiManagementReport;
import etiya.omniAutomation.business.dto.PerformanceInsightReport;
import etiya.omniAutomation.business.dto.PerformanceManagementReport;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.entity.PerfRsltEntity;
import etiya.omniAutomation.repository.PerformanceResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PerformanceAiReportRegenerationService {

    private final PerformanceResultRepository performanceResultRepository;
    private final PerformanceManagementReportBuilder performanceManagementReportBuilder;
    private final PerformanceInsightBuilder performanceInsightBuilder;
    private final PerformanceAiReportService performanceAiReportService;

    @Transactional
    public PerformanceAiManagementReport regenerate(
            Long performanceResultId,
            PerformanceThreadGroup threadDetail
    ) {
        if (performanceResultId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "performanceResultId is required.");
        }
        PerfRsltEntity result = performanceResultRepository.findById(performanceResultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Performance result not found: " + performanceResultId));
        PerformanceManagementReport managementReport = performanceManagementReportBuilder.build(
                result.getRunSummary(),
                result.getThresholdResult(),
                result.getAnalysisSummary(),
                result.getErrorAnalysis(),
                result.getEnvironmentMetrics(),
                result.getBaselineComparison(),
                result.getSummary()
        );
        PerformanceInsightReport insightReport = result.getInsightReport();
        if (insightReport == null) {
            insightReport = performanceInsightBuilder.build(
                    result.getRunSummary(),
                    result.getThresholdResult(),
                    result.getAnalysisSummary(),
                    result.getErrorAnalysis(),
                    result.getEnvironmentMetrics(),
                    result.getBaselineComparison(),
                    result.getSummary(),
                    threadDetail
            );
            result.setInsightReport(insightReport);
        }
        PerformanceAiManagementReport aiReport = performanceAiReportService.generate(
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
        result.setAiManagementReport(aiReport);
        performanceResultRepository.save(result);
        return aiReport;
    }
}
