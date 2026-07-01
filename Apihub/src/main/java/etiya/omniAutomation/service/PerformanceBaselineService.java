package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.entity.PerfRsltEntity;
import etiya.omniAutomation.repository.PerformanceResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PerformanceBaselineService {

    private final PerformanceResultRepository performanceResultRepository;
    private final PerformanceComparisonService performanceComparisonService;

    @Transactional
    public PerfRsltEntity setBaseline(Long performanceResultId) {
        PerfRsltEntity target = performanceResultRepository.findById(performanceResultId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Performance result not found: " + performanceResultId));
        if (target.getProjectId() == null || target.getProcessFlowId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Performance result must have projectId and processFlowId to become baseline.");
        }

        List<PerfRsltEntity> existingBaselines = performanceResultRepository.findByProjectIdAndProcessFlowIdAndBaselineTrue(
                target.getProjectId(),
                target.getProcessFlowId()
        );
        for (PerfRsltEntity existing : existingBaselines) {
            existing.setBaseline(false);
        }
        performanceResultRepository.saveAll(existingBaselines);

        target.setBaseline(true);
        return performanceResultRepository.save(target);
    }

    @Transactional(readOnly = true)
    public Optional<PerfRsltEntity> getBaseline(Long projectId, Long processFlowId) {
        return performanceResultRepository.findFirstByProjectIdAndProcessFlowIdAndBaselineTrue(projectId, processFlowId);
    }

    @Transactional
    public PerfRsltEntity applyAutomaticBaselineComparison(PerfRsltEntity completedRun) {
        if (completedRun == null || completedRun.getProjectId() == null || completedRun.getProcessFlowId() == null) {
            return completedRun;
        }

        Optional<PerfRsltEntity> baseline = getBaseline(completedRun.getProjectId(), completedRun.getProcessFlowId());
        if (baseline.isEmpty()) {
            return completedRun;
        }
        PerfRsltEntity baselineRun = baseline.get();
        if (baselineRun.getPerfRsltId() != null && baselineRun.getPerfRsltId().equals(completedRun.getPerfRsltId())) {
            return completedRun;
        }

        PerformanceComparisonResult comparison = performanceComparisonService.compare(baselineRun, completedRun);
        completedRun.setBaselineResultId(baselineRun.getPerfRsltId());
        completedRun.setBaselineComparison(comparison);
        return performanceResultRepository.save(completedRun);
    }
}
