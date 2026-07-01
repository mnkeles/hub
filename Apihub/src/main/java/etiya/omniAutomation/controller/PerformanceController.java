package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceAiManagementReport;
import etiya.omniAutomation.business.dto.PerformanceResultDto;
import etiya.omniAutomation.business.dto.PerformanceExportPayload;
import etiya.omniAutomation.business.dto.PerformanceLiveSnapshot;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceValidationChecklist;
import etiya.omniAutomation.request.PerformanceRequest;
import etiya.omniAutomation.request.PerformanceValidationNoteRequest;
import etiya.omniAutomation.results.PerformanceSummaryResult;
import etiya.omniAutomation.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    @PostMapping("/run")
    public ResponseEntity<PerformanceResultDto> runPerformanceTest(@RequestBody PerformanceRequest request) {
        return ResponseEntity.ok(this.performanceService.executePerformanceTest(request));
    }

    @GetMapping("/detail")
    public ResponseEntity<PerformanceThreadGroup> getPerformanceDetail(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.getDetail(performanceResultId));
    }

    @GetMapping("/getHistory")
    public ResponseEntity<List<PerformanceSummaryResult>> getHistory(@RequestParam Long projectId, @RequestParam Long processFlowId) {
        return ResponseEntity.ok(this.performanceService.getHistory(projectId, processFlowId));
    }

    @GetMapping("/getSummaries")
    public ResponseEntity<List<PerformanceSummary>> getSummaries(@RequestParam Long projectId, @RequestParam Long processFlowId) {
        List<PerformanceSummaryResult> history = this.performanceService.getHistory(projectId, processFlowId);
        if (history == null || history.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        // En son test sonucunun summary'lerini döndür
        return ResponseEntity.ok(history.get(0).performanceSummaries());
    }

    @GetMapping("/analysis")
    public ResponseEntity<PerformanceExportPayload> getAnalysis(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.getAnalysis(performanceResultId));
    }

    @GetMapping("/live")
    public ResponseEntity<PerformanceLiveSnapshot> getLive(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.getLiveSnapshot(performanceResultId));
    }

    @PostMapping("/stop")
    public ResponseEntity<PerformanceLiveSnapshot> stop(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.stop(performanceResultId, false));
    }

    @PostMapping("/force-stop")
    public ResponseEntity<PerformanceLiveSnapshot> forceStop(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.stop(performanceResultId, true));
    }

    @GetMapping("/compare")
    public ResponseEntity<PerformanceComparisonResult> compare(@RequestParam Long baseResultId, @RequestParam Long targetResultId) {
        return ResponseEntity.ok(this.performanceService.compare(baseResultId, targetResultId));
    }

    @PostMapping("/baseline")
    public ResponseEntity<PerformanceSummaryResult> setBaseline(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.setBaseline(performanceResultId));
    }

    @GetMapping("/baseline")
    public ResponseEntity<PerformanceSummaryResult> getBaseline(@RequestParam Long projectId, @RequestParam Long processFlowId) {
        return this.performanceService.getBaseline(projectId, processFlowId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/validation-note")
    public ResponseEntity<PerformanceValidationChecklist> updateValidationNote(@RequestBody PerformanceValidationNoteRequest request) {
        return ResponseEntity.ok(this.performanceService.updateValidationNote(request));
    }

    @PostMapping("/{performanceResultId}/ai-report/regenerate")
    public ResponseEntity<PerformanceAiManagementReport> regenerateAiReport(@PathVariable Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.regenerateAiReport(performanceResultId));
    }

    @GetMapping("/export")
    public ResponseEntity<?> export(@RequestParam Long performanceResultId, @RequestParam(defaultValue = "json") String format) {
        Object export = this.performanceService.export(performanceResultId, format);
        if ("csv".equalsIgnoreCase(format)) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=performance-" + performanceResultId + ".csv")
                    .body(export);
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(export);
    }
}
