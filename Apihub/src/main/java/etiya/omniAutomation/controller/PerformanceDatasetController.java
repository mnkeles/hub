package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.PerformanceDatasetDto;
import etiya.omniAutomation.business.dto.PerformanceDatasetPreview;
import etiya.omniAutomation.business.dto.PerformanceDatasetRowDto;
import etiya.omniAutomation.common.PermissionConstants;
import etiya.omniAutomation.request.PerformanceDatasetRequest;
import etiya.omniAutomation.request.PerformanceDatasetRowRequest;
import etiya.omniAutomation.service.PerformanceDatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/performance/datasets")
@RequiredArgsConstructor
public class PerformanceDatasetController {

    private final PerformanceDatasetService performanceDatasetService;

    @GetMapping
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
    public ResponseEntity<List<PerformanceDatasetDto>> list(@RequestParam Long projectId) {
        return ResponseEntity.ok(performanceDatasetService.list(projectId));
    }

    @GetMapping("/{datasetId}/preview")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_HISTORY_VIEW + "')")
    public ResponseEntity<PerformanceDatasetPreview> preview(@PathVariable Long datasetId) {
        return ResponseEntity.ok(performanceDatasetService.preview(datasetId));
    }

    @PostMapping
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceDatasetDto> create(@RequestBody PerformanceDatasetRequest request) {
        return ResponseEntity.ok(performanceDatasetService.create(request));
    }

    @PutMapping("/{datasetId}")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceDatasetDto> update(@PathVariable Long datasetId, @RequestBody PerformanceDatasetRequest request) {
        return ResponseEntity.ok(performanceDatasetService.update(datasetId, request));
    }

    @DeleteMapping("/{datasetId}")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<Void> deactivate(@PathVariable Long datasetId) {
        performanceDatasetService.deactivate(datasetId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{datasetId}/rows")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceDatasetRowDto> addRow(@PathVariable Long datasetId, @RequestBody PerformanceDatasetRowRequest request) {
        return ResponseEntity.ok(performanceDatasetService.addRow(datasetId, request));
    }

    @PutMapping("/{datasetId}/rows/{rowId}")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceDatasetRowDto> updateRow(@PathVariable Long datasetId, @PathVariable Long rowId, @RequestBody PerformanceDatasetRowRequest request) {
        return ResponseEntity.ok(performanceDatasetService.updateRow(datasetId, rowId, request));
    }

    @DeleteMapping("/{datasetId}/rows/{rowId}")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<Void> deactivateRow(@PathVariable Long datasetId, @PathVariable Long rowId) {
        performanceDatasetService.deactivateRow(datasetId, rowId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERFORMANCE_TEST_RUN + "')")
    public ResponseEntity<PerformanceDatasetDto> upload(
            @RequestParam Long projectId,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String defaultMapping,
            @RequestParam MultipartFile file
    ) {
        return ResponseEntity.ok(performanceDatasetService.upload(projectId, name, description, defaultMapping, file));
    }
}
