package etiya.omniAutomation.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import etiya.omniAutomation.business.dto.ParallelCallRequestDto;
import etiya.omniAutomation.business.dto.ParameterRequestDto;
import etiya.omniAutomation.service.ApiCallServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiCallController {

    private final ApiCallServiceImpl apiCallService;

    @GetMapping("/callStep/{project}/{systemShortCode}/{stepShortCode}")
    public ResponseEntity<String> callXMLApi(@PathVariable String project, @PathVariable String systemShortCode, @PathVariable String stepShortCode) {
        return this.apiCallService.callXMLApi(project, systemShortCode, stepShortCode, false);
    }

    @GetMapping("/callProcess/{project}/{systemShortCode}/{processFlow}")
    public ResponseEntity<Map<String, Object>> callXmlProcessFlow(@PathVariable String project, @PathVariable String systemShortCode ,@PathVariable String processFlow) {
        return this.apiCallService.callXmlProcessFlow(project, systemShortCode, processFlow, false);
    }

    @PostMapping("/callProcess/{project}/{systemShortCode}/{processFlow}/v2")
    public ResponseEntity<Map<String, Object>> callXmlProcessFlowWithParams(@PathVariable String project,
                                                                            @PathVariable String systemShortCode,
                                                                            @PathVariable String processFlow,
                                                                            @RequestBody(required = false) ParameterRequestDto parameterRequest,
                                                                            boolean continueOnError) throws JsonProcessingException {
        if (Objects.nonNull(parameterRequest) && parameterRequest.isCombination())
            return this.apiCallService.combinationCallXmlProcessFlowWithParameterContext(project, systemShortCode, processFlow, false, parameterRequest);
        return this.apiCallService.callXmlProcessFlowWithParameterContext(project, systemShortCode, processFlow, false, continueOnError, parameterRequest);
    }

    @PostMapping("/callProcess/{project}/{systemShortCode}/{processFlow}/parallel")
    public ResponseEntity<Map<String, Object>> parallelCallXmlProcessFlowWithParams(@PathVariable String project,
                                                                                    @PathVariable String systemShortCode,
                                                                                    @PathVariable String processFlow,
                                                                                    @RequestBody(required = false) ParallelCallRequestDto parallelCallRequestDto) {
        return this.apiCallService.parallelCallXmlProcessFlowWithParameterContext(project, systemShortCode, processFlow, parallelCallRequestDto);
    }

}


