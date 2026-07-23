package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.PerformanceComparisonResult;
import etiya.omniAutomation.business.dto.PerformanceResultDto;
import etiya.omniAutomation.business.dto.PerformanceExportPayload;
import etiya.omniAutomation.business.dto.PerformanceLiveSnapshot;
import etiya.omniAutomation.business.dto.PerformanceSummary;
import etiya.omniAutomation.business.dto.PerformanceThreadGroup;
import etiya.omniAutomation.business.dto.PerformanceValidationChecklist;
import etiya.omniAutomation.common.PermissionConstants;
import etiya.omniAutomation.request.PerformanceRequest;
import etiya.omniAutomation.request.PerformanceValidationNoteRequest;
import etiya.omniAutomation.results.PerformanceSummaryResult;
import etiya.omniAutomation.service.PerformanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceService performanceService;

    @PostMapping("/run")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceResultDto> runPerformanceTest(@RequestBody PerformanceRequest request) {
        return ResponseEntity.ok(this.performanceService.executePerformanceTest(request));
    }

    @GetMapping("/detail")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
    public ResponseEntity<PerformanceThreadGroup> getPerformanceDetail(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.getDetail(performanceResultId));
    }

    @GetMapping("/getHistory")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
    public ResponseEntity<List<PerformanceSummaryResult>> getHistory(@RequestParam Long projectId, @RequestParam Long processFlowId) {
        return ResponseEntity.ok(this.performanceService.getHistory(projectId, processFlowId));
    }

    @GetMapping("/getSummaries")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
    public ResponseEntity<List<PerformanceSummary>> getSummaries(@RequestParam Long projectId, @RequestParam Long processFlowId) {
        List<PerformanceSummaryResult> history = this.performanceService.getHistory(projectId, processFlowId);
        if (history == null || history.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        // En son test sonucunun summary'lerini döndür
        return ResponseEntity.ok(history.get(0).performanceSummaries());
    }

    @GetMapping("/analysis")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
    public ResponseEntity<PerformanceExportPayload> getAnalysis(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.getAnalysis(performanceResultId));
    }

    @GetMapping("/live")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
    public ResponseEntity<PerformanceLiveSnapshot> getLive(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.getLiveSnapshot(performanceResultId));
    }

    @PostMapping("/stop")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceLiveSnapshot> stop(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.stop(performanceResultId, false));
    }

    @PostMapping("/force-stop")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceLiveSnapshot> forceStop(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.stop(performanceResultId, true));
    }

    @GetMapping("/compare")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
    public ResponseEntity<PerformanceComparisonResult> compare(@RequestParam Long baseResultId, @RequestParam Long targetResultId) {
        return ResponseEntity.ok(this.performanceService.compare(baseResultId, targetResultId));
    }

    @PostMapping("/baseline")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceSummaryResult> setBaseline(@RequestParam Long performanceResultId) {
        return ResponseEntity.ok(this.performanceService.setBaseline(performanceResultId));
    }

    @GetMapping("/baseline")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
    public ResponseEntity<PerformanceSummaryResult> getBaseline(@RequestParam Long projectId, @RequestParam Long processFlowId) {
        return this.performanceService.getBaseline(projectId, processFlowId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/validation-note")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceValidationChecklist> updateValidationNote(@RequestBody PerformanceValidationNoteRequest request) {
        return ResponseEntity.ok(this.performanceService.updateValidationNote(request));
    }

    @GetMapping("/export")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
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
