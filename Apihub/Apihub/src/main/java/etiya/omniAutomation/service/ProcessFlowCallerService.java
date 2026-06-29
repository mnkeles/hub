package etiya.omniAutomation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.ParameterRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class ProcessFlowCallerService {

    @Autowired
    private ApiCallServiceImpl apiCallService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<Map<String, Object>> callProcessFlow(String project, String systemShortCode, String processFlow,
                                        String parameterContextJson, String globalHeadersJson) {
        try {
            if (project == null || project.trim().isEmpty()) {
                throw new IllegalArgumentException("Project boş olamaz");
            }
            if (systemShortCode == null || systemShortCode.trim().isEmpty()) {
                throw new IllegalArgumentException("System Short Code boş olamaz");
            }
            if (processFlow == null || processFlow.trim().isEmpty()) {
                throw new IllegalArgumentException("Process Flow boş olamaz");
            }

            ParameterRequestDto requestDto = new ParameterRequestDto();

            if (parameterContextJson != null && !parameterContextJson.trim().isEmpty()) {
                try {
                    Map<String, Object> parameterContext = objectMapper.readValue(parameterContextJson, Map.class);
                    requestDto.setParameterContext(parameterContext);
                } catch (Exception e) {
                    throw new RuntimeException("Parameter Context JSON formatı geçersiz: " + e.getMessage());
                }
            }

            if (globalHeadersJson != null && !globalHeadersJson.trim().isEmpty()) {
                try {
                    Map<String, Object> globalHeaders = objectMapper.readValue(globalHeadersJson, Map.class);
                    requestDto.setGlobalHeaders(globalHeaders);
                } catch (Exception e) {
                    throw new RuntimeException("Global Headers JSON formatı geçersiz: " + e.getMessage());
                }
            }

            return apiCallService.callXmlProcessFlowWithParameterContext(project,systemShortCode,processFlow,false, false, requestDto);

        } catch (Exception e) {
            throw new RuntimeException("Process flow çağırma hatası: " + e.getMessage(), e);
        }
    }
}
