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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiCallServiceImplTest {

    @Autowired
    private ApiCallServiceImpl apiCallService;

    @ParameterizedTest(name = "API TEST WITH ONLY STEP : {0}")
    //@ValueSource(strings = {"odf-retrieveSubscriptionSeq","odf-assertMsisdnReferencedAssetSeq","odf-retrieveHistoricalLimitSeq"})
    @MethodSource("getSource")
    @Description("MTS API TEST")
    @Owner("Alperen ÖZTÜRK !")
    @Severity(SeverityLevel.CRITICAL)
    @Feature("API Testing")
    @Story("Verify API functionality with different parameters")
    @Epic("API Test")
    void callXMLApi(String stepShortCode, String projectShortCode) {
        String systemShortCodeOAB = System.getProperty("systemShortCodeOAB", "OABdev");
        ResponseEntity<String> response = apiCallService.callXMLApi(projectShortCode, systemShortCodeOAB, stepShortCode, false);
        // Response'un null olup olmadığını kontrol et
        String responseBody = response.getBody();

        if (Objects.nonNull(responseBody)) {
            if (responseBody.startsWith("{")) { // JSON formatında kontrol
                this.processJsonResponse(responseBody);
            } else { // Tanınmayan format veya hatalı durum
                if (!response.getStatusCode().is2xxSuccessful()) {
                    logError("Unexpected HTTP status: " + response.getStatusCode() + " | Body: " + responseBody);
                }
                assertTrue(response.getStatusCode().is2xxSuccessful(), "Response status code is not 2xx.");
            }
        }

        logResponseBody(responseBody, stepShortCode);
    }

    private void processJsonResponse(String responseBody) {
        try {
            if (responseBody == null || responseBody.isBlank()) {
                logError("Response body is null or empty.");
                fail("Response body should not be null or empty.");
                return;
            }

            // JSON Dizi (JSONArray) mi kontrol et
            if (responseBody.startsWith("[")) {
                JSONArray jsonArray = new JSONArray(responseBody);

                // JSON dizisi boş veya ilk elemanı null ya da boş mu?
                if (jsonArray.length() == 0 || jsonArray.opt(0) == null || jsonArray.optString(0).isBlank()) {
                    logError("Boş veya geçersiz JSON dizisi alındı.");
                    fail("JSON dizisi boş olmamalıdır.");
                } else {
                    System.out.println("JSON Dizisi alındı, boyut: " + jsonArray.length());

                    // İlk elemanın JSONObject olup olmadığını güvenli şekilde kontrol et
                    JSONObject firstObject = jsonArray.optJSONObject(0);
                    if (firstObject != null) {
                        processResponseObject(firstObject); // İlk objeyi işle
                    } else {
                        logError("JSON dizisinin ilk elemanı beklenen formatta değil.");
                        fail("JSON dizisinin ilk elemanı JSONObject olmalıdır.");
                    }
                }
                return;
            }

            JSONObject jsonResponse = new JSONObject(responseBody);

            // ReturnCode 500 ve Internal Server Error kontrolü
            if (jsonResponse.optInt("returnCode", 200) == 500 &&
                    "Internal Server Error".equalsIgnoreCase(jsonResponse.optString("returnMessage", ""))) {
                logError("Internal Server Error detected: " + responseBody);
                fail("API returned Internal Server Error.");
                return;
            }

            // **preqRes kontrolü**
            if (jsonResponse.has("preqRes") && jsonResponse.isNull("preqRes")) {
                logError("preqRes null olarak döndü.");
                fail("preqRes null olmamalıdır.");
                return;
            }

            // 1. requestResponse kontrolü
            if (jsonResponse.has("requestResponse")) {
                Object requestResponse = jsonResponse.get("requestResponse");

                if (requestResponse instanceof JSONArray) {
                    JSONArray requestResponseArray = (JSONArray) requestResponse;
                    if (requestResponseArray.length() > 0) {
                        processResponseObject(requestResponseArray.getJSONObject(0));
                    } else {
                        logError("Boş requestResponse dizisi");
                        fail("Boş requestResponse dizisi");
                    }
                } else if (requestResponse instanceof JSONObject) {
                    processResponseObject((JSONObject) requestResponse);
                } else {
                    logError("Beklenmeyen requestResponse tipi");
                    fail("Beklenmeyen requestResponse tipi");
                }
            }
            // 2. operationResult kontrolü
            else if (jsonResponse.has("operationResult")) {
                JSONObject operationResult = jsonResponse.getJSONObject("operationResult");
                boolean result = operationResult.optBoolean("successResult", false);
                assertTrue(result);
            }
            // 3. externalRequestResponse kontrolü
            else if (jsonResponse.has("externalRequestResponse")) {
                JSONObject externalResponse = jsonResponse.getJSONObject("externalRequestResponse");
                JSONObject resultStatus = externalResponse.getJSONObject("resultStatus");
                int resultCode = resultStatus.getInt("resultCode");

                if (resultCode == 0) {
                    System.out.println("BİLGİ: resultCode: 0 olan yanıt başarılı kabul edildi");
                } else {
                    if (resultStatus.has("resultMessage")) {
                        Object resultMessage = resultStatus.get("resultMessage");

                        if (resultMessage instanceof JSONArray) {
                            JSONArray messages = (JSONArray) resultMessage;
                            if (messages.length() > 0) {
                                JSONObject firstMessage = messages.getJSONObject(0);
                                int resultStringCode = firstMessage.optInt("resultStringCode", -1);
                                assertEquals(100, resultStringCode, "Beklenmeyen resultStringCode değeri");
                            }
                        } else if (resultMessage instanceof JSONObject) {
                            JSONObject msgObj = (JSONObject) resultMessage;
                            int resultStringCode = msgObj.getInt("resultStringCode");
                            assertEquals(100, resultStringCode, "Beklenmeyen resultStringCode değeri");
                        }
                    } else {
                        fail("resultCode 0 olmadığında resultMessage zorunludur");
                    }
                }
            }

        } catch (JSONException e) {
            logError("Geçersiz JSON yanıtı: " + e.getMessage());
            fail("JSON yanıtı işlenemedi");
        }
    }

    private void processResponseObject(JSONObject responseObj) {
        int actualReturnCode = responseObj.optInt("returnCode", 500);
        String actualResultCode = responseObj.optString("returnMessage","GENERIC ERROR");

        // ReturnCode ve ResultMessage doğrulamaları
        if (actualReturnCode != 100 || !"SUCCESS".equalsIgnoreCase(actualResultCode)) {
            logError("Unexpected ReturnCode or ResultMessage: " + responseObj.toString());
        }

        // Beklenen değerlerin kontrolü
        assertEquals(100, actualReturnCode, "ReturnCode is not as expected.");
        assertEquals("SUCCESS", actualResultCode, "ResultMessage is not as expected.");
    }

    static Stream<Arguments> getSource() {
        String values = System.getProperty("stepValues", "");
        String project = System.getProperty("project", "OMNI");
        return Arrays.stream(values.split(","))
                .map(value -> Arguments.of(value, project));
    }

    static Stream<Arguments> getProcessSource() {
        String values = System.getProperty("processValues", "");
        String project = System.getProperty("project", "OMNI");
        return Arrays.stream(values.split(",")).map(value -> Arguments.of(value, project));
    }

    // Allure raporlama için yanıt gövdesini loglama
    @Step("Response Body: {responseName}")
    private void logResponseBody(String responseBody, String responseName) {
        Allure.addAttachment("Response Body: " + responseName, "text/plain", responseBody);
    }

    // Allure raporlama için hata loglama
    @Step("ErrorResponse: {errorResponse}")
    private void logError(String errorMessage) {
        Allure.addAttachment("ERROR Response", "text/plain", errorMessage);
    }

    @ParameterizedTest(name = "PROCESS FLOW API TEST : {0}")
    @MethodSource("getProcessSource")
    //@ValueSource(strings = {"odf-commonOrderSeq","odf-commonOrderSeq-iss_transfer_in","odf-commonOrderSeq-ssi"})
    @Description("PROCESS FLOW API TEST AUTOMATION")
    @Owner("Alperen ÖZTÜRK !")
    @Severity(SeverityLevel.BLOCKER)
    @Feature("PROCESS FLOW")
    @Story("Verify API functionality with different identity types")
    @Epic("API Test")
    @Tags({@Tag("API"), @Tag("IdentityVerification"), @Tag("Regression")})
    public void callXmlProcessFlow(String processFlow, String project) {
        String systemShortCodeOAB = System.getProperty("systemShortCodeOAB", "OABdev");
        ResponseEntity<Map<String, Object>> response = this.apiCallService.callXmlProcessFlow(project, systemShortCodeOAB, processFlow, true);

        // Response'un null olup olmadığını kontrol et
        assertNotNull(response, "API response is null.");

        int actualReturnCode = 0;
        // Response'un ReturnCode değerini kontrol et
        if (Objects.nonNull(response.getBody())) {
            Map<String, Object> body = response.getBody();
            // Yeni response yapısından result'ı al
            Map<String, String> resultMap = (Map<String, String>) body.get("result");

            for (Map.Entry<String, String> responseBody : resultMap.entrySet()) {
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

    // XML geçerliliğini kontrol eden metot
    private boolean isValidXml(String content) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(new ByteArrayInputStream(content.getBytes()));
            return true; // Geçerli XML
        } catch (Exception e) {
            return false; // Geçersiz XML
        }
    }

    static Stream<Arguments> getProcessFlowE2E() {
        String values = System.getProperty("E2EprocessFlow", "createCustomer"); // Default: createCustomer
        String project = System.getProperty("project", "OMNI");
        
        // Jenkins'ten gelen runCount parametresini oku (default: 20)
        int runCount = Integer.parseInt(System.getProperty("runCount", "20"));
        
        // Eğer E2EprocessFlow verilmişse veya default kullanılıyorsa, her değeri runCount kere tekrarla
        if (values != null && !values.isEmpty()) {
            return Arrays.stream(values.split(","))
                    .flatMap(value -> java.util.stream.IntStream.rangeClosed(1, runCount)
                            .mapToObj(runNumber -> Arguments.of(value, project, runNumber)));
        }
        
        // Değer yoksa boş stream döndür
        return Stream.empty();
    }

    @ParameterizedTest(name = "PROCESS FLOW N/N API TEST : {0} - Run #{2}")
    @MethodSource("getProcessFlowE2E")
    //@ValueSource(strings = {"createCustomer","createForeignCustomer"})
    @Description("PROCESS FLOW N/N API TEST AUTOMATION - Multiple Runs")
    @Owner("Alperen ÖZTÜRK !")
    @Severity(SeverityLevel.BLOCKER)
    @Feature("PROCESS E2E Test")
    @Story("Verify API functionality with different identity types")
    @Epic("API Test")
    @Tags({@Tag("API"), @Tag("IdentityVerification"), @Tag("Regression")})
    public void callXmlProcessFlowE2E(String processFlow, String project, int runNumber) {
        // Allure'da run number'ı göster
        Allure.parameter("Process Flow", processFlow);
        Allure.parameter("Project", project);
        Allure.parameter("Run Number", runNumber);
        Allure.step("Executing run #" + runNumber + " for E2E process flow: " + processFlow);
        
        String systemShortCode = System.getProperty("systemShortCode", "test"); // Varsayılan "OMNI"
        ResponseEntity<Map<String, Object>> response = apiCallService.callXmlProcessFlow(project, systemShortCode, processFlow, true);

        // Response'un null olup olmadığını kontrol et
        assertNotNull(response, "API response is null.");

        int actualReturnCode = 0;
        // Response'un ReturnCode değerini kontrol et
        if (Objects.nonNull(response.getBody())) {
            Map<String, Object> body = response.getBody();
            // Yeni response yapısından result'ı al
            Map<String, String> resultMap = (Map<String, String>) body.get("result");

            for (Map.Entry<String, String> responseBody : resultMap.entrySet()) {
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
}
