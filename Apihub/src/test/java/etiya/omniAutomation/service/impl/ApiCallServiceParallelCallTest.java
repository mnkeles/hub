package etiya.omniAutomation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.ParallelCallRequestDto;
import etiya.omniAutomation.business.dto.ParameterRequestDto;
import etiya.omniAutomation.service.ApiCallServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ApiCallServiceParallelCallTest {
    @Autowired
    private ApiCallServiceImpl apiCallService;
    
    private static final List<TestResult> testResults = new LinkedList<>();

    @ParameterizedTest(name = "PROCESS FLOW N/N API TEST : {0}")
    @MethodSource("getProcessFlowE2E")
    public void callXmlProcessFlowWithParameterContext(String processFlow, String project, String systemShortCode, ParameterRequestDto parameterRequest, int totalCalls) throws JsonProcessingException {
        logTestInfo(processFlow, project, systemShortCode, totalCalls);
        
        ParallelCallRequestDto parallelCallRequestDto = new ParallelCallRequestDto(parameterRequest, totalCalls);
        ResponseEntity<Map<String, Object>> response = apiCallService.parallelCallXmlProcessFlowWithParameterContext(project, systemShortCode, processFlow, parallelCallRequestDto);

        assertNotNull(response, "API response is null.");
        
        storeTestResult(processFlow, project, systemShortCode, totalCalls, response);
    }
    
    private void logTestInfo(String processFlow, String project, String systemShortCode, int totalCalls) {
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║              TEST BAŞLATILDI                               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("  Process Flow      : " + processFlow);
        System.out.println("  Project           : " + project);
        System.out.println("  System Short Code : " + systemShortCode);
        System.out.println("  Total Calls       : " + totalCalls);
        System.out.println("════════════════════════════════════════════════════════════\n");
    }
    
    private void storeTestResult(String processFlow, String project, String systemShortCode, int totalCalls, ResponseEntity<Map<String, Object>> response) {
        TestResult result = new TestResult();
        result.processFlow = processFlow;
        result.project = project;
        result.systemShortCode = systemShortCode;
        result.totalCalls = totalCalls;
        result.response = response;
        testResults.add(result);
    }
    
    @AfterAll
    static void printAllResults() {
        System.out.println("\n\n");
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           TÜM TEST SONUÇLARI                               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println();
        
        ObjectMapper objectMapper = new ObjectMapper();
        int testNumber = 1;
        
        for (TestResult result : testResults) {
            System.out.println("\n╔════════════════════════════════════════════════════════════╗");
            System.out.println("║              TEST #" + testNumber + "                                      ║");
            System.out.println("╚════════════════════════════════════════════════════════════╝");
            System.out.println("  Process Flow      : " + result.processFlow);
            System.out.println("  Project           : " + result.project);
            System.out.println("  System Short Code : " + result.systemShortCode);
            System.out.println("  Total Calls       : " + result.totalCalls);
            System.out.println("════════════════════════════════════════════════════════════\n");
            
            try {
                String responseJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.response.getBody());
                
                System.out.println("═══════════════════════════════════════════════════════════");
                System.out.println("Process Flow: " + result.processFlow);
                System.out.println("HTTP Status: " + result.response.getStatusCode());
                System.out.println("═══════════════════════════════════════════════════════════");
                System.out.println("Response Body:");
                System.out.println(responseJson);
                System.out.println("═══════════════════════════════════════════════════════════\n");
            } catch (Exception e) {
                System.err.println("Response loglama hatası: " + e.getMessage());
            }
            
            testNumber++;
        }
        
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║           TOPLAM " + testResults.size() + " TEST TAMAMLANDI                       ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
    }
    
    private static class TestResult {
        String processFlow;
        String project;
        String systemShortCode;
        int totalCalls;
        ResponseEntity<Map<String, Object>> response;
    }

    static Stream<Arguments> getProcessFlowE2E() {
        String processFlowValues = System.getProperty("processFlow", "createCustomer");
        String project = System.getProperty("project", "DARWIN");
        String systemShortCodeValues = System.getProperty("systemShortCode", "DRWP-UAT");
        int totalCalls = Integer.parseInt(System.getProperty("totalCalls", "20"));
        
        // ParameterRequestDto için property kontrolü
        ParameterRequestDto paramRequest;
        String parameterRequestJson = System.getProperty("parameterRequest");
        
        if (parameterRequestJson != null && !parameterRequestJson.trim().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                paramRequest = objectMapper.readValue(parameterRequestJson, ParameterRequestDto.class);
                System.out.println("[INFO] ParameterRequestDto property'den alındı: " + parameterRequestJson);
            } catch (Exception e) {
                System.err.println("[WARNING] ParameterRequestDto parse edilemedi, boş oluşturuluyor. Hata: " + e.getMessage());
                paramRequest = new ParameterRequestDto();
            }
        } else {
            System.out.println("[INFO] ParameterRequestDto property bulunamadı, boş oluşturuluyor.");
            paramRequest = new ParameterRequestDto();
        }

        ParameterRequestDto finalParamRequest = paramRequest;
        
        // systemShortCode değerlerini boşluk veya virgülle ayır
        String[] systemShortCodes = systemShortCodeValues.split("[\\s,]+");
        
        // processFlow değerlerini virgülle ayır
        String[] processFlows = processFlowValues.split(",");
        
        // Her systemShortCode ve processFlow kombinasyonu için test oluştur
        return Arrays.stream(systemShortCodes)
                .flatMap(systemShortCode -> 
                    Arrays.stream(processFlows)
                        .map(processFlow -> Arguments.of(processFlow.trim(), project, systemShortCode.trim(), finalParamRequest, totalCalls))
                );
    }
}