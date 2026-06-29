package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.ProcessFlowStepParmDto;
import etiya.omniAutomation.results.Result;
import etiya.omniAutomation.service.ProcessFlowServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/process-flow-step-parm")
@RequiredArgsConstructor
@Slf4j
public class ProcessFlowStepParmController {

    private final ProcessFlowServiceImpl processFlowService;

    @PutMapping("/update")
    public ResponseEntity<Result> updateParameter(@RequestBody ProcessFlowStepParmDto processFlowStepParmDto) {
        try {
            Result result = processFlowService.updateProcessFlowStepParameter(processFlowStepParmDto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error updating process flow step parameter", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Process Flow Step Parameter service is running");
    }
}
