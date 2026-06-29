package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.ProcessFlowDto;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.service.ProcessFlowServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process-flow")
@RequiredArgsConstructor
@Slf4j
public class ProcessFlowController {

    private final ProcessFlowServiceImpl processFlowService;

    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> getAll(@RequestBody(required = false) GeneralPageRequest pageRequest) {
        try {
            if (pageRequest == null) {
                pageRequest = new GeneralPageRequest(0, 100);
            }
            List<ProcessFlowDto> processFlows = processFlowService.getAll(pageRequest);
            long totalCount = processFlowService.count(pageRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", processFlows);
            response.put("totalCount", totalCount);
            response.put("offset", pageRequest.getOffset());
            response.put("limit", pageRequest.getLimit());
            
            log.info("Fetched {} process flows out of {}", processFlows.size(), totalCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching process flows", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{processFlowId}")
    public ResponseEntity<ProcessFlowDto> getById(@PathVariable Long processFlowId) {
        try {
            ProcessFlowDto processFlow = processFlowService.getByProcessFlowId(processFlowId);
            return ResponseEntity.ok(processFlow);
        } catch (Exception e) {
            log.error("Error fetching process flow by id: {}", processFlowId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{processFlowId}/with-relations")
    public ResponseEntity<ProcessFlowDto> getByIdWithRelations(@PathVariable Long processFlowId) {
        try {
            ProcessFlowDto processFlow = processFlowService.findByIdWithRelations(processFlowId);
            return ResponseEntity.ok(processFlow);
        } catch (Exception e) {
            log.error("Error fetching process flow with relations by id: {}", processFlowId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProcessFlowDto>> getAllSimple() {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            List<ProcessFlowDto> processFlows = processFlowService.getAll(pageRequest);
            log.info("Fetched {} process flows", processFlows.size());
            return ResponseEntity.ok(processFlows);
        } catch (Exception e) {
            log.error("Error fetching all process flows", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProcessFlowDto>> getByProjectId(@PathVariable Long projectId) {
        try {
            List<ProcessFlowDto> processFlows = processFlowService.getFlowsByProject(projectId);
            return ResponseEntity.ok(processFlows);
        } catch (Exception e) {
            log.error("Error fetching process flows by project id: {}", projectId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Result> save(@RequestBody ProcessFlowDto processFlowDto) {
        try {
            Result result = processFlowService.save(processFlowDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error saving process flow", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Result> update(@RequestBody ProcessFlowDto processFlowDto) {
        try {
            Result result = processFlowService.update(processFlowDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating process flow", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{processFlowId}")
    public ResponseEntity<Result> delete(@PathVariable Long processFlowId) {
        try {
            Result result = processFlowService.deleteProcessFlow(processFlowId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error deleting process flow: {}", processFlowId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/copy/{processFlowId}")
    public ResponseEntity<Result> copyProcessFlow(@PathVariable Long processFlowId) {
        try {
            log.info("Copying process flow with id: {}", processFlowId);
            Result result = processFlowService.copyProcessFlow(processFlowId);
            if (result.isSuccess()) {
                log.info("Process flow copied successfully: {}", result.getMessage());
            } else {
                log.error("Failed to copy process flow: {}", result.getMessage());
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error copying process flow: {}", processFlowId, e);
            return ResponseEntity.ok(new etiya.omniAutomation.results.ErrorResult("Akış kopyalama hatası: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Process Flow service is running");
    }
}
