package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.PerformanceResultDto;
import etiya.omniAutomation.business.dto.PerformanceScheduleDto;
import etiya.omniAutomation.common.PermissionConstants;
import etiya.omniAutomation.request.PerformanceScheduleRequest;
import etiya.omniAutomation.service.PerformanceScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/performance/schedules")
@RequiredArgsConstructor
public class PerformanceScheduleController {

    private final PerformanceScheduleService performanceScheduleService;

    @GetMapping
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
    public ResponseEntity<List<PerformanceScheduleDto>> list(@RequestParam Long projectId, @RequestParam Long processFlowId) {
        return ResponseEntity.ok(performanceScheduleService.list(projectId, processFlowId));
    }

    @PostMapping
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceScheduleDto> create(@RequestBody PerformanceScheduleRequest request) {
        return ResponseEntity.ok(performanceScheduleService.create(request));
    }

    @PutMapping("/{scheduleId}")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceScheduleDto> update(@PathVariable Long scheduleId, @RequestBody PerformanceScheduleRequest request) {
        return ResponseEntity.ok(performanceScheduleService.update(scheduleId, request));
    }

    @PostMapping("/{scheduleId}/enabled")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceScheduleDto> setEnabled(@PathVariable Long scheduleId, @RequestParam boolean enabled) {
        return ResponseEntity.ok(performanceScheduleService.setEnabled(scheduleId, enabled));
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<Void> deactivate(@PathVariable Long scheduleId) {
        performanceScheduleService.deactivate(scheduleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{scheduleId}/run-now")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceResultDto> runNow(@PathVariable Long scheduleId) {
        return ResponseEntity.ok(performanceScheduleService.runNow(scheduleId));
    }
}
