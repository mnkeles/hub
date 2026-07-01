package etiya.omniAutomation.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import etiya.omniAutomation.business.dto.ParameterRequestDto;
import etiya.omniAutomation.service.ApiCallServiceImpl;
import io.qameta.allure.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
public class ApiCallServiceTest {
    @Autowired
    private ApiCallServiceImpl apiCallService;

    @ParameterizedTest(name = "PROCESS FLOW N/N API TEST : {0}")
    @MethodSource("getProcessFlowE2E")
    //@ValueSource(strings = {"createCustomer","createForeignCustomer"})
    @Description("PROCESS FLOW N/N API TEST AUTOMATION")
    @Severity(SeverityLevel.BLOCKER)
    @Feature("PROCESS E2E Test")
    @Story("Verify API functionality with different identity types")
    @Epic("API Test")
    @Tags({@Tag("API"), @Tag("IdentityVerification"), @Tag("Regression")})
    public void callXmlProcessFlowWithParameterContext(String processFlow, String project,String systemShortCode, ParameterRequestDto parameterRequest) throws JsonProcessingException {
        ResponseEntity<Map<String, Object>> response = apiCallService.callXmlProcessFlowWithParameterContext(project, systemShortCode, processFlow, false, false,parameterRequest);

        assertNotNull(response, "API response is null.");

        int actualReturnCode = 0;

        if (Objects.nonNull(response.getBody())) {
            Map<String, String> body = (Map<String, String>) response.getBody().get("Result");

            for (Map.Entry<String, String> responseBody : body.entrySet()) {
                if (isValidXml(responseBody.getValue())) { // XML geçerliliğini kontrol et
                    Pattern pattern = Pattern.compile("ReturnCode>(\\d+)<"); // ReturnCode için regex deseni
                    Matcher matcher = pattern.matcher(responseBody.getValue());
                    actualReturnCode = matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;

                    if (actualReturnCode != 100) {
                        logError("ReturnCode is not 100: " + responseBody);
                    }
                    assertEquals(100, actualReturnCode, "ReturnCode is not as expected.");
                } else {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        logError("Unexpected response status code: " + response.getStatusCode());
                    }
                    assertTrue(response.getStatusCode().is2xxSuccessful(), "Response status code is not 2xx.");
                }
                logResponseBody(responseBody.getValue(), responseBody.getKey());
            }
        }
    }
    
    @Step("Response Body: {responseName}")
    private void logResponseBody(String responseBody, String responseName) {
        Allure.addAttachment("Response Body: " + responseName, "text/plain", responseBody);
    }

    @Step("ErrorResponse: {errorResponse}")
    private void logError(String errorMessage) {
        Allure.addAttachment("ERROR Response", "text/plain", errorMessage);
    }

    private boolean isValidXml(String content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new ByteArrayInputStream(content.getBytes()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static Stream<Arguments> getProcessFlowE2E() {
        String values = System.getProperty("processFlow", "createCustomer");
        String project = System.getProperty("project", "DARWIN");
        String systemShortCode = System.getProperty("systemShortCode", "DRWP-UAT");
        ParameterRequestDto paramRequest = new ParameterRequestDto();

        return Arrays.stream(values.split(","))
                .map(processFlow -> Arguments.of(processFlow,project,systemShortCode,paramRequest));
    }
}