package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAiReport;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.entity.PerfRsltEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PerformanceReportSnapshotService {

    private final PerformanceAiReportService performanceAiReportService;

    public PerformanceReportSnapshot build(
            PerfRsltEntity result,
            PerformanceThreadGroup threadDetail
    ) {
        PerformanceAiReport aiReport = performanceAiReportService.generateReport(result, threadDetail);
        return new PerformanceReportSnapshot(aiReport);
    }

    public record PerformanceReportSnapshot(
            PerformanceAiReport aiReport
    ) {
    }
}
