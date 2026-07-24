package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceAiReport;
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
    private final PerformanceAiReportService performanceAiReportService;

    @Transactional
    public PerformanceAiReport regenerate(
            Long performanceResultId,
            PerformanceThreadGroup threadDetail
    ) {
        if (performanceResultId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "performanceResultId is required.");
        }
        PerfRsltEntity result = performanceResultRepository.findById(performanceResultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Performance result not found: " + performanceResultId));
        PerformanceAiReport aiReport = performanceAiReportService.generateReport(result, threadDetail);
        result.setAiReport(aiReport);
        performanceResultRepository.save(result);
        return aiReport;
    }
}
