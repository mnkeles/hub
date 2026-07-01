package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.ProcessFlowStepDto;
import etiya.omniAutomation.business.dto.ProcessFlowStepRelationDto;
import etiya.omniAutomation.request.GeneralPageRequest;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.service.ProcessFlowStepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/process-flow-step")
@RequiredArgsConstructor
@Slf4j
public class ProcessFlowStepController {

    private final ProcessFlowStepService processFlowStepService;

    @PostMapping("/list")
    public ResponseEntity<Map<String, Object>> getAll(@RequestBody(required = false) GeneralPageRequest pageRequest) {
        try {
            if (pageRequest == null) {
                pageRequest = new GeneralPageRequest(0, 100);
            }
            List<ProcessFlowStepDto> steps = processFlowStepService.getProcessFlowSteps(pageRequest);
            long totalCount = processFlowStepService.count(pageRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", steps);
            response.put("totalCount", totalCount);
            response.put("offset", pageRequest.getOffset());
            response.put("limit", pageRequest.getLimit());
            
            log.info("Fetched {} process flow steps out of {}", steps.size(), totalCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching process flow steps", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProcessFlowStepDto>> getAllSimple() {
        try {
            GeneralPageRequest pageRequest = new GeneralPageRequest(0, 1000);
            List<ProcessFlowStepDto> steps = processFlowStepService.getProcessFlowSteps(pageRequest);
            log.info("Fetched {} process flow steps", steps.size());
            return ResponseEntity.ok(steps);
        } catch (Exception e) {
            log.error("Error fetching all process flow steps", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/relations/{processFlowStepId}/{projectId}")
    public ResponseEntity<List<ProcessFlowStepRelationDto>> getRelations(
            @PathVariable Long processFlowStepId,
            @PathVariable Long projectId) {
        try {
            List<ProcessFlowStepRelationDto> relations = 
                processFlowStepService.getProcessFlowStepRelation(processFlowStepId, projectId);
            return ResponseEntity.ok(relations);
        } catch (Exception e) {
            log.error("Error fetching process flow step relations", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/save")
    public ResponseEntity<Result> save(@RequestBody ProcessFlowStepDto processFlowStepDto) {
        try {
            Result result = processFlowStepService.save(processFlowStepDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error saving process flow step", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Result> update(@RequestBody ProcessFlowStepDto processFlowStepDto) {
        try {
            Result result = processFlowStepService.update(processFlowStepDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating process flow step", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{processFlowStepId}")
    public ResponseEntity<Void> delete(@PathVariable Long processFlowStepId) {
        try {
            processFlowStepService.delete(processFlowStepId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting process flow step: {}", processFlowStepId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/parameter/{relationId}")
    public ResponseEntity<Void> deleteParameter(@PathVariable Long relationId) {
        try {
            processFlowStepService.deleteProcessFlowStepParameter(relationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting process flow step parameter: {}", relationId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/create-step")
    public ResponseEntity<Void> createNewProcessStep(@RequestBody ProcessFlowStepRelationDto relationDto) {
        try {
            processFlowStepService.createNewProcessStep(relationDto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error creating new process step", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/update-orders")
    public ResponseEntity<Result> updateStepOrders(@RequestBody List<ProcessFlowStepDto> steps) {
        try {
            Result result = processFlowStepService.updateStepOrders(steps);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating step orders", e);
            return ResponseEntity.status(500)
                .body(new Result(false, "Sıralama güncellenemedi"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Process Flow Step service is running");
    }
}
