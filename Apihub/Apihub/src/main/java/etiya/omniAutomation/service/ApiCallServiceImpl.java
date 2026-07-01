package etiya.omniAutomation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import etiya.omniAutomation.business.dto.*;
import etiya.omniAutomation.common.*;
import etiya.omniAutomation.entity.*;
import etiya.omniAutomation.mappers.ProcessFlowMapper;
import etiya.omniAutomation.mappers.ProcessFlowStepMapper;
import etiya.omniAutomation.repository.DefaultRequestRepository;
import etiya.omniAutomation.repository.PerformanceResultItemRepository;
import etiya.omniAutomation.repository.PerformanceResultRepository;
import etiya.omniAutomation.repository.ProcessFlowRepository;
import io.qameta.allure.Allure;

import java.nio.charset.StandardCharsets;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;
import java.lang.String;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApiCallServiceImpl {
    private final WebClientService webClientService;
    private final SystemEndpoint systemEndpoint;
    private final ProcessFlowStepService processFlowStepService;
    public final UtilityService utilityService;
    private final ProcessFlowRepository processFlowRepository;
    public final DatabaseHelper databaseHelper;
    public final ExecutorService virtualThreadExecutor;
    private final PerformanceResultItemRepository performanceResultItemRepository;
    private final PerformanceResultRepository performanceResultRepository;
    private final ProjectService projectService;
    private final GRPCService grpcService;
    private final PlatformTransactionManager platformTransactionManager;
    private TransactionTemplate transactionTemplate;
    private final DefaultRequestRepository defaultRequestRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiLogServiceImpl apiLogService;
    private final PerformanceRunRegistry performanceRunRegistry;
    private final PerformanceBaselineService performanceBaselineService;
    private final PerformanceValidationChecklistBuilder performanceValidationChecklistBuilder;
    private final PerformanceReportSnapshotService performanceReportSnapshotService;

    private static final Logger log = LoggerFactory.getLogger(ApiCallServiceImpl.class);

    @PostConstruct
    void startUp() {
        transactionTemplate = new TransactionTemplate(platformTransactionManager);
    }

    public ResponseEntity<String> callXMLApi(String projectShortCode, String systemShortCodeOAB, String stepShortCode, boolean fromTest) {
        ProjectDto project = this.projectService.getProject(projectShortCode);
        List<Long> flowIds = this.processFlowRepository.findProcessFlowIdsByProjectId(project.getProjectId());
        ProcessFlowStepEntity stepEntity = this.processFlowStepService.inquireProcessStepByStepShortCode(stepShortCode, flowIds);
        ProcessFlowStepDto step = ProcessFlowStepMapper.INSTANCE.toDto(stepEntity);
        ApiInformationDto apiInformation = step.getApiInformation();
        apiInformation.setShortCode(systemShortCodeOAB);
        ProcessFlowDto processFlow = this.processFlowRepository.findById(stepEntity.getProcessFlowId())
                .map(ProcessFlowMapper.INSTANCE::toDtoWithoutRelations)
                .orElseThrow(() -> new IllegalArgumentException("ProcessFlow not found"));
        this.processRequestBody(step, apiInformation, processFlow);
        ResponseEntity<String> result = this.sendApiInformationXML(apiInformation, step, fromTest, project.getProjectId(), processFlow.getParameterContext());
        logApiCall(project.getName(), result.getStatusCode().value(),
                this.systemEndpoint.find(apiInformation.getShortCode() + "_" + project.getProjectId()) + apiInformation.getSrvcName(),
                apiInformation.getPlIn(), result.getBody());
        return result;
    }

    public ResponseEntity<Map<String, Object>> callXmlProcessFlowCallApi(ProjectDto project , String systemShortCode, ProcessFlowDto processFlow, boolean fromTest) {
        processFlow.setSystemShortCode(systemShortCode);
        Map<String, String> resultMap = new LinkedHashMap<>();
        Map<String, String> errorInfo = null;
        
        for (ProcessFlowStepDto step : processFlow.getProcessFlowStepList()) {
            ApiInformationDto apiInformation = step.getApiInformation();
            if (apiInformation.isTokenApi()) {
                String shortCode = project.getGeneralWebSystemDtoList().stream()
                        .filter(url -> Objects.nonNull(url.getBaseUrlShortCode())
                                && url.getBaseUrlShortCode().equals(systemShortCode))
                        .findAny()
                        .map(GeneralWebSystemDto::getShortCode)
                        .orElse(systemShortCode);

                apiInformation.setShortCode(shortCode);
            } else {
                apiInformation.setShortCode(systemShortCode);
            }

            this.processRequestBody(step, apiInformation, processFlow);
            ResponseEntity<String> result;
            if(apiInformation.isSqlQuery()) {
                result = executeSqlQuery(step, systemShortCode, apiInformation, processFlow.getParameterContext(), project.getProjectId());
            } else {
                result = this.sendApiInformationXML(apiInformation, step, fromTest, project.getProjectId(), processFlow.getParameterContext());
            }
            if (result.getStatusCode().is2xxSuccessful()) {
                if (step.getApiInformation().isTokenApi()) {
                    String bearerToken = "Bearer " + extractAccessTokenFromOAuthResponse(result.getBody());
                    processFlow.getGlobalHeaders().put("Authorization", bearerToken);
                } else {
                    processFlow.processHeaders(result, step.getHeaderExtractor());
                }
                processFlow.processParameters(result, step.getParameterExtractor());
                if (!apiInformation.isSqlQuery()) {
                    logApiCall(project.getName(), result.getStatusCode().value(),
                            this.systemEndpoint.find(apiInformation.getShortCode() + "_" + project.getProjectId()) + apiInformation.getSrvcName(),
                            apiInformation.getPlIn(), result.getBody());
                }
                resultMap.put(step.getStepShortCode(), StringUtils.deleteWhitespace(result.getBody()));
            } else {
                resultMap.put(step.getStepShortCode(), StringUtils.deleteWhitespace(result.getBody()));
                errorInfo = handleErrorResponse(step, result, apiInformation, project);
                break;
            }
        }

        return buildFinalResponse(resultMap, processFlow.getParameterContext(), errorInfo);
    }

    public ResponseEntity<Map<String, Object>> callXmlProcessFlow(String projectShortCode, String systemShortCode, String processFlowShortCode, boolean fromTest) {
        ProjectDto project = this.projectService.getProject(projectShortCode);
        ProcessFlowEntity processFlowEntity = this.processFlowRepository.getByShortCodeAndProjectId(processFlowShortCode, project.getProjectId());
        ProcessFlowDto processFlow = ProcessFlowMapper.INSTANCE.toDto(processFlowEntity);
        processFlow.getGlobalHeaders().put("Collation", "tr");
        return callXmlProcessFlowCallApi(project, systemShortCode, processFlow, fromTest);
    }

    public ResponseEntity<Map<String, Object>> callXmlProcessFlowWithParameterContext(String projectShortCode, String systemShortCode, String processFlowShortCode,
                                                                                      boolean fromTest, boolean continueOnError, ParameterRequestDto parameterRequest) throws JsonProcessingException {
        ProjectDto project = this.projectService.getProject(projectShortCode);
        ProcessFlowEntity processFlowEntity = this.processFlowRepository.getByShortCodeAndProjectId(processFlowShortCode, project.getProjectId());
        ProcessFlowDto processFlow = ProcessFlowMapper.INSTANCE.toDto(processFlowEntity);
        if (Objects.isNull(parameterRequest) || Objects.isNull(parameterRequest.getParameterContext()) || parameterRequest.getParameterContext().isEmpty()) {
            DefaultRequestEntity defaultRequest = defaultRequestRepository.findDefaultRequest(projectShortCode, systemShortCode, processFlowShortCode);
            parameterRequest = new ParameterRequestDto();
            if (!Objects.isNull(defaultRequest)) {
                if (defaultRequest.getParameterContext() != null && !defaultRequest.getParameterContext().trim().isEmpty()) {
                    Map<String, Object> parameterContext = objectMapper.readValue(defaultRequest.getParameterContext(), Map.class);
                    parameterRequest.setParameterContext(parameterContext);
                }
                if (defaultRequest.getGlobalHeaders() != null && !defaultRequest.getGlobalHeaders().trim().isEmpty()) {
                    Map<String, Object> globalHeaders = objectMapper.readValue(defaultRequest.getGlobalHeaders(), Map.class);
                    parameterRequest.setGlobalHeaders(globalHeaders);
                }
            }
        }

        Optional.ofNullable(parameterRequest.getParameterContext())
                .ifPresent(parameterContext -> parameterContext.forEach((key, value) -> {
                    String stringValue = value != null ? value.toString() : null;
                    processFlow.getParameterContext().put(key, stringValue);
                }));
        Optional.ofNullable(parameterRequest.getGlobalHeaders())
                .ifPresent(globalHeaders -> globalHeaders.forEach((key, value) -> {
                    String stringValue = value != null ? value.toString() : null;
                    processFlow.getGlobalHeaders().put(key, stringValue);
                }));

        return callXmlProcessFlowCallApi2(project, systemShortCode, processFlow, fromTest, continueOnError);
    }

    public ResponseEntity<Map<String, Object>> combinationCallXmlProcessFlowWithParameterContext(String projectShortCode, String systemShortCode, String processFlowShortCode,
                                                                                                 boolean fromTest, ParameterRequestDto parameterRequest) throws JsonProcessingException {

        Map<String, Object> resultMap = new LinkedHashMap<>();
        List<Map<String, Object>> combinations = generateAllCombinations(parameterRequest.getParameterContext());

        ExecutorService executorService = Executors.newFixedThreadPool(3);
        AtomicInteger count = new AtomicInteger(1);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();

        for (Map<String, Object> combination : combinations) {
            Future<Map<String, Object>> future = executorService.submit(() -> {
                try {
                    ParameterRequestDto newRequest = new ParameterRequestDto();
                    newRequest.setParameterContext(combination);
                    newRequest.setGlobalHeaders(parameterRequest.getGlobalHeaders());

                    ResponseEntity<Map<String, Object>> response = callXmlProcessFlowWithParameterContext(
                            projectShortCode, systemShortCode, processFlowShortCode, fromTest, false, newRequest);
                    Map<String, Object> responseBody = Optional.ofNullable(response.getBody()).orElse(new LinkedHashMap<>());

                    int currentCount = count.getAndIncrement();

                    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
                    /*@SuppressWarnings("unchecked")
                    Optional<Map<String, Object>> parametrelerOpt = Optional.ofNullable(responseBody.get("Parametreler"))
                            .filter(value -> value instanceof Map)
                            .map(value -> (Map<String, Object>) value);
                    
                    parametrelerOpt.flatMap(parametrelerMap -> Optional.ofNullable(parametrelerMap.get("customerId")))
                            .ifPresent(customerId -> result.put(currentCount+"-customerId", customerId));*/

                    Optional<Object> firstValue = responseBody.entrySet().stream()
                            .findFirst()
                            .map(Map.Entry::getValue);
                    if (firstValue.isPresent() && firstValue.get() instanceof Map) {
                        Map<String, String> innerMap = (Map<String, String>) firstValue.get();
                        innerMap.entrySet().stream()
                                .reduce((first, second) -> second)
                                .ifPresent(lastEntry -> result.put(currentCount + "-" +lastEntry.getKey(), lastEntry.getValue()));
                    }
                    responseBody.entrySet().stream()
                            .reduce((first, second) -> second)
                            .ifPresent(lastEntry -> result.put(currentCount + "-" +lastEntry.getKey(), lastEntry.getValue()));

                    return result;
                } catch (Exception e) {
                    return new LinkedHashMap<>();
                }
            });
            futures.add(future);
        }

        for (Future<Map<String, Object>> future : futures) {
            try {
                Map<String, Object> threadResult = future.get();
                resultMap.putAll(threadResult);
            } catch (InterruptedException | ExecutionException e) {
                log.error("Thread execution failed: {}", e.getMessage(), e);
            }
        }
        executorService.shutdown();

        return ResponseEntity.ok().body(resultMap);
    }

    private List<Map<String, Object>> generateAllCombinations(Map<String, Object> baseContext) {
        if (Objects.isNull(baseContext)) return List.of(new HashMap<>());

        Map<String, Object> parsedContext = new HashMap<>();
        for (Map.Entry<String, Object> entry : baseContext.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.startsWith("[") && strValue.endsWith("]")) {
                    String content = strValue.substring(1, strValue.length() - 1);
                    if (!content.trim().isEmpty()) {
                        List<Object> parsedList = Arrays.stream(content.split(","))
                                .map(String::trim)
                                .collect(Collectors.toList());
                        parsedContext.put(entry.getKey(), parsedList);
                        continue;
                    }
                }
            }
            parsedContext.put(entry.getKey(), value);
        }

        List<String> listFields = parsedContext.entrySet().stream()
                .filter(e -> e.getValue() instanceof List)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (listFields.isEmpty()) return List.of(parsedContext);

        List<Map<String, Object>> results = new ArrayList<>();
        generateRecursive(parsedContext, listFields, 0, new HashMap<>(), results);
        return results;
    }

    private void generateRecursive(Map<String, Object> base, List<String> fields,
                                   int index, Map<String, Object> current,
                                   List<Map<String, Object>> results) {
        if (index == fields.size()) {
            Map<String, Object> combo = new HashMap<>(base);
            combo.putAll(current);
            results.add(combo);
            return;
        }

        List<?> values = (List<?>) base.get(fields.get(index));
        for (Object value : values) {
            Map<String, Object> newCurrent = new HashMap<>(current);
            newCurrent.put(fields.get(index), value);
            generateRecursive(base, fields, index + 1, newCurrent, results);
        }
    }

    public String extractAccessTokenFromOAuthResponse(String response) {
        return JsonPath.read(response, "$.access_token");
    }

    public ResponseEntity<Map<String, Object>> callXmlProcessFlowE2E(String projectShortCode, String systemShortCode, String procFlowShortCode, boolean fromTest) {
        ProjectDto project = this.projectService.getProject(projectShortCode);
        ProcessFlowEntity processFlowEntity = this.processFlowRepository.getByShortCodeAndProjectId(procFlowShortCode, project.getProjectId());
        ProcessFlowDto processFlow = ProcessFlowMapper.INSTANCE.toDto(processFlowEntity);
        processFlow.setSystemShortCode(systemShortCode);
        Map<String, String> resultMap = new HashMap<>();
        Map<String, String> errorInfo = null;

        for (ProcessFlowStepDto step : processFlow.getProcessFlowStepList()) {
            ApiInformationDto apiInformation = step.getApiInformation();
            apiInformation.setShortCode(systemShortCode);
            this.processRequestBody(step, apiInformation, processFlow);
            ResponseEntity<String> result = this.sendApiInformationXML(apiInformation, step, fromTest, project.getProjectId(), processFlow.getParameterContext());
            if (result.getStatusCode().is2xxSuccessful()) {
                processFlow.processHeaders(result, step.getHeaderExtractor());
                processFlow.processParameters(result, step.getParameterExtractor());
                resultMap.put(step.getStepShortCode(), StringUtils.deleteWhitespace(result.getBody()));
            } else {
                resultMap.put(step.getStepShortCode(), StringUtils.deleteWhitespace(result.getBody()));
                errorInfo = handleErrorResponse(step, result, null, null);
                break;
            }
        }
        return buildFinalResponse(resultMap, processFlow.getParameterContext(), errorInfo);
    }

    private Map<String, String> handleErrorResponse(ProcessFlowStepDto step, ResponseEntity<String> result,
                                                     ApiInformationDto apiInformation, ProjectDto project) {
        Map<String, String> errorInfo = new LinkedHashMap<>();
        errorInfo.put("step", step.getStepShortCode());
        errorInfo.put("statusCode", String.valueOf(result.getStatusCode().value()));
        errorInfo.put("message", result.getBody() != null && result.getBody().length() <= 500
                ? result.getBody() : "Error response too long");

        if (project != null && apiInformation != null) {
            ApiLogEntity apiLogEntity = new ApiLogEntity(project.getName(),
                    result.getStatusCode().value(),
                    this.systemEndpoint.find(apiInformation.getShortCode() + "_" + project.getProjectId()) + apiInformation.getSrvcName(),
                    apiInformation.getPlIn(), result.getBody());
            apiLogService.save(apiLogEntity);
        }

        return errorInfo;
    }

    private void logApiCall(String projectName, int statusCode, String apiUrl, String plIn, String plOut) {
        try {
            apiLogService.save(new ApiLogEntity(projectName, statusCode, apiUrl, plIn, plOut));
        } catch (Exception e) {
            log.warn("Failed to save API call log: {}", e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> buildFinalResponse(Map<String, String> resultMap,
                                                                    Map<String, String> parameterContext,
                                                                    Map<String, String> errorInfo) {
        Map<String, Object> filteredParameterContext = new LinkedHashMap<>();
        parameterContext.entrySet().stream()
                .filter(entry -> entry.getValue() == null || entry.getValue().length() <= 500)
                .forEach(entry -> filteredParameterContext.put(entry.getKey(), entry.getValue()));
        
        if (errorInfo != null) {
            filteredParameterContext.put("error", errorInfo);
        }

        Map<String, Object> finalResponse = new LinkedHashMap<>();
        finalResponse.put("result", resultMap);
        finalResponse.put("parameterContext", filteredParameterContext);
        return ResponseEntity.ok().body(finalResponse);
    }

    private ResponseEntity<Map<String, Object>> buildFinalResponseForDarwin(Map<String, String> resultMap,
                                                                             Map<String, String> parameterContext,
                                                                             Map<String, String> errorInfo) {
        Map<String, Object> filteredParameterContext = new LinkedHashMap<>();
        parameterContext.entrySet().stream()
                .filter(entry -> entry.getValue() == null || entry.getValue().length() <= 300)
                .forEach(entry -> filteredParameterContext.put(entry.getKey(), entry.getValue()));

        if (errorInfo != null) {
            filteredParameterContext.put("error", errorInfo);
        }

        Map<String, Object> finalResponse = new LinkedHashMap<>();
        finalResponse.put("Result", resultMap);
        finalResponse.put("Parametreler", filteredParameterContext);
        return ResponseEntity.ok().body(finalResponse);
    }

    private void processRequestBody(ProcessFlowStepDto step, ApiInformationDto apiInformation, ProcessFlowDto processFlow) {
        apiInformation.setHeaderVal(step.getStepShortCode());
        apiInformation.setPlIn(step.getPlIn());

        step.getProcessFlowStepParmList()
                .stream()
                .sorted(Comparator.comparing(ProcessFlowStepParmDto::getParamOrder, Comparator.naturalOrder()))
                .forEach(item -> {
                    if (item.isUseContext())
                        this.changePlIn(apiInformation, item, processFlow, new ExpressionInput(processFlow, this.utilityService));
                    else
                        this.changePlIn(apiInformation, item, processFlow, null);
                });

        Optional.ofNullable(processFlow)
                .map(ProcessFlowDto::getGlobalHeaders)
                .filter(item -> !item.isEmpty())
                .ifPresent(item -> apiInformation.getGlobalHeaders().putAll(item));
    }

    private void changePlIn(ApiInformationDto apiInformation, ProcessFlowStepParmDto stepParam, ProcessFlowDto processFlow, Object rootObject) {
        ExpressionParser expressionParser = new SpelExpressionParser();
        if (StringUtils.isNotEmpty(stepParam.getValExpression())) {
            Expression expression = expressionParser.parseExpression(stepParam.getValExpression());
            EvaluationContext context = new StandardEvaluationContext(Objects.nonNull(rootObject) ? rootObject : this);
            context.setVariable("systemShortCodeOAB", processFlow.getSystemShortCodeOAB());
            context.setVariable("systemShortCode", processFlow.getSystemShortCode());
            context.setVariable("currentStepParameter", stepParam);
            context.setVariable("currentFlow", processFlow);
            context.setVariable("currentApiInformation", apiInformation);
            context.setVariable("projectId", processFlow.getProjectId());
            String value = expression.getValue(context, String.class);
            stepParam.setValue(value);
        }
        // Null kontrolleri - stepParam.getShortCode() ve stepParam.getValue() null olabilir
        String shortCode = Optional.ofNullable(stepParam.getShortCode()).orElse("");
        String paramValue = Optional.ofNullable(stepParam.getValue()).orElse("");
        String plIn = Optional.ofNullable(apiInformation.getPlIn()).orElse(shortCode);
        
        if (!shortCode.isEmpty()) {
            apiInformation.setPlIn(plIn.replace(shortCode, paramValue));
            Optional.ofNullable(processFlow)
                    .map(ProcessFlowDto::getParameterContext)
                    .ifPresent(item -> item.put(shortCode.replaceFirst("\\$\\{(\\w+)\\}", "$1"), paramValue));
        }
    }

    public ResponseEntity<String> sendApiInformationXML(ApiInformationDto apiInformation, ProcessFlowStepDto processFlowStep, boolean fromTest, Long projectId, Map<String, String> parameterContext) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(apiInformation.getMediaType()));
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

        apiInformation.getGlobalHeaders().forEach(headers::add);
        if (StringUtils.isNotEmpty(apiInformation.getHeaderParameters())) {
            headers.add(apiInformation.getHeaderParameters(), apiInformation.getHeaderVal());
        }

        if (StringUtils.isNotEmpty(processFlowStep.getPreHeader())) {
            Arrays.stream(processFlowStep.getPreHeader().split(";"))
                    .forEach(item -> {
                        String[] headerItem = item.split("&");
                        if (StringUtils.equalsIgnoreCase(headerItem[0], "Authorization_Basic")) {
                            byte[] encodedAuth = Base64.getEncoder().encode(headerItem[1].getBytes(StandardCharsets.UTF_8));
                            String headerValue = "Basic " + new String(encodedAuth);
                            headers.set("Authorization", headerValue);
                        } else {
                            headers.set(headerItem[0], headerItem[1]);
                        }
                    });
        }
        String url = this.systemEndpoint.find(apiInformation.getShortCode() + "_" + projectId);

        if (apiInformation.isGrpc()) {
            return this.sendApiInformationGRPC(url, apiInformation);
        }
        url += apiInformation.getSrvcName();

        if(apiInformation.isExternalApi()){
            url = apiInformation.getSrvcName();
        }

        url = replaceUrlPlaceholders(url, parameterContext);

        HttpEntity<String> httpEntity = null;

        String method = apiInformation.getHttpMethod();
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        if (!httpMethod.matches(HttpMethod.GET.name())) {
            httpEntity = new HttpEntity<>(apiInformation.getPlIn(), headers);
        }

        //çağrı yapılan yer
        Allure.addAttachment("Request: " + processFlowStep.getStepShortCode(), apiInformation.getPlIn() + "\nURL: " + url);
        ResponseEntity<String> response;
        try{
            response = this.webClientService.exchange(url, httpEntity, headers, httpMethod, new ParameterizedTypeReference<String>() {});
        } catch (WebClientResponseException e) {
            response = ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
         return response;
    }
    private String replaceUrlPlaceholders(String url, Map<String, String> parameterContext) {
        if (url == null || !url.contains("${")) {
            return url;
        }
        String result = url;
        int startIndex = 0;
        while ((startIndex = result.indexOf("${", startIndex)) != -1) {
            int endIndex = result.indexOf("}", startIndex);
            if (endIndex != -1) {
                String key = result.substring(startIndex + 2, endIndex);
                String value = parameterContext.get(key);

                if (value != null) {
                    result = result.substring(0, startIndex) + value + result.substring(endIndex + 1);
                    startIndex += value.length();
                } else {
                    startIndex = endIndex + 1;
                }
            } else {
                break;
            }
        }
        return result;
    }

    public ResponseEntity<String> sendApiInformationGRPC(String url, ApiInformationDto apiInformation) {
        try {
            return this.grpcService.callGrpc(url, apiInformation);
        } catch (InterruptedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    private ProcessFlowDto copyProcessFlowForThread(ProcessFlowDto source) {
        return objectMapper.convertValue(source, ProcessFlowDto.class);
    }

    private long calculateRampUpDelayMillis(int threadNumber, int threadCount, int rampUpPeriodSeconds) {
        if (threadCount <= 1 || rampUpPeriodSeconds <= 0) {
            return 0;
        }
        return Math.round(threadNumber * ((rampUpPeriodSeconds * 1000.0) / threadCount));
    }

    private void processPerformanceTask(ProcessFlowDto processFlowDto,
                                        List<PerformanceResultItemDto> stepList,
                                        int threadNumber,
                                        int threadCount,
                                        int rampUpPeriodSeconds,
                                        PerformanceResultDto performanceResultDto) {
        long rampUpDelayMillis = calculateRampUpDelayMillis(threadNumber, threadCount, rampUpPeriodSeconds);
        if (rampUpDelayMillis > 0) {
            try {
                Thread.sleep(rampUpDelayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                markRunningStepsStopped(stepList, "Stopped by user");
                return;
            }
        }

        ProcessFlowDto threadProcessFlow;
        try {
            threadProcessFlow = copyProcessFlowForThread(processFlowDto);
        } catch (Exception e) {
            markRemainingStepsFailed(stepList, "Thread flow copy failed: " + e.getMessage());
            return;
        }

        int loopLimit = Optional.ofNullable(performanceResultDto.getLoopCount()).filter(value -> value > 0).orElse(1);
        Integer durationSeconds = Optional.ofNullable(performanceResultDto.getDurationSeconds()).filter(value -> value > 0).orElse(null);
        Integer thinkTimeMs = Optional.ofNullable(performanceResultDto.getThinkTimeMs()).filter(value -> value > 0).orElse(null);
        long deadlineMillis = durationSeconds == null ? Long.MAX_VALUE : System.currentTimeMillis() + (durationSeconds * 1000L);
        int sampleIndex = 0;

        for (int loop = 0; loop < loopLimit && System.currentTimeMillis() <= deadlineMillis; loop++) {
            if (isCancellationRequested(performanceResultDto.getPerformanceResultId())) {
                markRunningStepsStopped(stepList, "Stopped by user");
                return;
            }
            for (ProcessFlowStepRelationDto stepRelation : threadProcessFlow.getProcessFlowStepRelations()) {
                if (System.currentTimeMillis() > deadlineMillis || isCancellationRequested(performanceResultDto.getPerformanceResultId())) {
                    markRunningStepsStopped(stepList, "Stopped by user");
                    return;
                }

                ProcessFlowStepDto step = stepRelation.getProcessFlowStep();
                ApiInformationDto apiInformation = step.getApiInformation();
                PerformanceResultItemDto performanceResultItemDto = sampleForExecution(stepList, sampleIndex++, stepRelation, threadNumber);
                performanceResultItemDto.setStepName(step.getStepShortCode());
                performanceResultItemDto.setThreadNumber(threadNumber);

                StopWatch stopWatch = new StopWatch();
                ResponseEntity<String> result;
                try {
                    apiInformation.setShortCode(threadProcessFlow.getSystemShortCode());
                    this.processRequestBody(step, apiInformation, threadProcessFlow);
                    performanceResultItemDto.setStartedAt(new Date());
                    stopWatch.start();
                    result = this.sendApiInformationXML(apiInformation, step, false, threadProcessFlow.getProjectId(), threadProcessFlow.getParameterContext());
                    stopWatch.stop();
                    performanceResultItemDto.setFinishedAt(new Date());
                } catch (Exception e) {
                    if (stopWatch.isStarted()) {
                        stopWatch.stop();
                    }
                    performanceResultItemDto.setFinishedAt(new Date());
                    performanceResultItemDto.setElapsedTime(((double) stopWatch.getTime(TimeUnit.MILLISECONDS)));
                    performanceResultItemDto.setPerformanceItemStatus(GeneralEnums.PerformanceStatus.FAILED);
                    performanceResultItemDto.setErrorMessage(e.getMessage());
                    markRemainingStepsFailed(stepList, performanceResultItemDto, "Skipped after previous step failure");
                    return;
                }

                performanceResultItemDto.setElapsedTime(((double) stopWatch.getTime(TimeUnit.MILLISECONDS)));
                if (result.getStatusCode().is2xxSuccessful()) {
                    threadProcessFlow.processHeaders(result, step.getHeaderExtractor());
                    threadProcessFlow.processParameters(result, step.getParameterExtractor());
                    performanceResultItemDto.setPerformanceItemStatus(GeneralEnums.PerformanceStatus.COMPLETED);
                } else {
                    performanceResultItemDto.setPerformanceItemStatus(GeneralEnums.PerformanceStatus.FAILED);
                    performanceResultItemDto.setErrorMessage(result.getBody());
                    markRemainingStepsFailed(stepList, performanceResultItemDto, "Skipped after previous step failure");
                    return;
                }
                sleepThinkTime(thinkTimeMs, stepList);
            }
        }
    }

    private PerformanceResultItemDto sampleForExecution(List<PerformanceResultItemDto> stepList,
                                                        int sampleIndex,
                                                        ProcessFlowStepRelationDto stepRelation,
                                                        int threadNumber) {
        if (sampleIndex < stepList.size()) {
            PerformanceResultItemDto item = stepList.get(sampleIndex);
            item.setProcessFlowStepId(stepRelation.getProcessFlowStepId());
            item.setStepName(stepRelation.getProcessFlowStep().getStepShortCode());
            item.setThreadNumber(threadNumber);
            return item;
        }
        PerformanceResultItemDto item = new PerformanceResultItemDto();
        item.setProcessFlowStepId(stepRelation.getProcessFlowStepId());
        item.setStepName(stepRelation.getProcessFlowStep().getStepShortCode());
        item.setThreadNumber(threadNumber);
        item.setPerformanceItemStatus(GeneralEnums.PerformanceStatus.RUNNING);
        stepList.add(item);
        return item;
    }

    private boolean isCancellationRequested(Long performanceResultId) {
        return performanceRunRegistry.find(performanceResultId)
                .map(PerformanceRunRegistry.PerformanceRunControl::isCancellationRequested)
                .orElse(false);
    }

    private void sleepThinkTime(Integer thinkTimeMs, List<PerformanceResultItemDto> stepList) {
        if (thinkTimeMs == null || thinkTimeMs <= 0) {
            return;
        }
        try {
            Thread.sleep(thinkTimeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markRunningStepsStopped(stepList, "Stopped by user");
        }
    }

    private void markRemainingStepsFailed(List<PerformanceResultItemDto> stepList, String errorMessage) {
        stepList.stream()
                .filter(item -> item.getPerformanceItemStatus() == GeneralEnums.PerformanceStatus.RUNNING)
                .forEach(item -> {
                    item.setPerformanceItemStatus(GeneralEnums.PerformanceStatus.FAILED);
                    item.setErrorMessage(errorMessage);
                });
    }

    private void markRemainingStepsFailed(List<PerformanceResultItemDto> stepList,
                                          PerformanceResultItemDto failedItem,
                                          String errorMessage) {
        boolean failedItemSeen = false;
        for (PerformanceResultItemDto item : stepList) {
            if (item == failedItem) {
                failedItemSeen = true;
                continue;
            }
            if (failedItemSeen && item.getPerformanceItemStatus() == GeneralEnums.PerformanceStatus.RUNNING) {
                item.setPerformanceItemStatus(GeneralEnums.PerformanceStatus.FAILED);
                item.setErrorMessage(errorMessage);
            }
        }
    }

    private void markRunningStepsStopped(List<PerformanceResultItemDto> stepList, String errorMessage) {
        stepList.stream()
                .filter(item -> item.getPerformanceItemStatus() == GeneralEnums.PerformanceStatus.RUNNING)
                .forEach(item -> {
                    item.setPerformanceItemStatus(GeneralEnums.PerformanceStatus.STOPPED);
                    item.setFinishedAt(new Date());
                    item.setErrorMessage(errorMessage);
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeFlowPerformanceTest(ProcessFlowDto processFlowDto, PerformanceResultDto performanceResultDto) {
        PerformanceThreadGroup runningItem = performanceResultDto.getThreadGroup();
        ConcurrentHashMap<Integer, List<PerformanceResultItemDto>> resultMap = runningItem.groups().stream()
                .collect(Collectors.toMap(PerformanceThread::threadNumber, PerformanceThread::steps,
                        (x1, x2) -> x1, ConcurrentHashMap::new));
        Map<Long, String> stepShortCodeMap = processFlowDto.getProcessFlowStepRelations()
                .stream()
                .map(ProcessFlowStepRelationDto::getProcessFlowStep)
                .collect(Collectors.toMap(ProcessFlowStepDto::getProcessFlowStepId, ProcessFlowStepDto::getStepShortCode,
                        (x1, x2) -> x1));

        int rawRampUp = Optional.ofNullable(performanceResultDto.getRampUpPeriod()).orElse(0);
        int threadCount = Optional.ofNullable(performanceResultDto.getThreadCount()).orElse(resultMap.size());

        List<CompletableFuture<?>> futures = resultMap.entrySet().stream()
                .map(entry ->
                        CompletableFuture.runAsync(() -> this.processPerformanceTask(processFlowDto, entry.getValue(), entry.getKey(), threadCount, rawRampUp, performanceResultDto), this.virtualThreadExecutor)
                )
                .collect(Collectors.toList());
        performanceRunRegistry.register(performanceResultDto.getPerformanceResultId(), futures);
        try {
            CompletableFuture<?>[] completableFutures = futures.toArray(CompletableFuture[]::new);
            CompletableFuture.allOf(completableFutures).get();
            boolean cancelled = performanceRunRegistry.find(performanceResultDto.getPerformanceResultId())
                    .map(PerformanceRunRegistry.PerformanceRunControl::isCancellationRequested)
                    .orElse(false);
            boolean forceCancelled = performanceRunRegistry.find(performanceResultDto.getPerformanceResultId())
                    .map(PerformanceRunRegistry.PerformanceRunControl::isForceCancellationRequested)
                    .orElse(false);
            persistPerformanceResult(performanceResultDto, runningItem, resultMap, stepShortCodeMap,
                    cancelled ? GeneralEnums.PerformanceStatus.STOPPED : GeneralEnums.PerformanceStatus.COMPLETED,
                    forceCancelled);
        } catch (InterruptedException | ExecutionException e) {
            boolean cancelled = performanceRunRegistry.find(performanceResultDto.getPerformanceResultId())
                    .map(PerformanceRunRegistry.PerformanceRunControl::isCancellationRequested)
                    .orElse(false);
            boolean forceCancelled = performanceRunRegistry.find(performanceResultDto.getPerformanceResultId())
                    .map(PerformanceRunRegistry.PerformanceRunControl::isForceCancellationRequested)
                    .orElse(false);
            persistPerformanceResult(performanceResultDto, runningItem, resultMap, stepShortCodeMap,
                    cancelled ? GeneralEnums.PerformanceStatus.STOPPED : GeneralEnums.PerformanceStatus.ERROR,
                    forceCancelled);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException(e);
        } finally {
            performanceRunRegistry.unregister(performanceResultDto.getPerformanceResultId());
        }
    }

    private void persistPerformanceResult(PerformanceResultDto performanceResultDto,
                                          PerformanceThreadGroup runningItem,
                                          ConcurrentHashMap<Integer, List<PerformanceResultItemDto>> resultMap,
                                          Map<Long, String> stepShortCodeMap,
                                          GeneralEnums.PerformanceStatus status,
                                          boolean forceStopped) {
        if (status == GeneralEnums.PerformanceStatus.STOPPED) {
            resultMap.values().forEach(stepList -> markRunningStepsStopped(stepList, "Stopped by user"));
        }
        List<PerformanceResultItemDto> allSamples = resultMap.values().stream()
                .flatMap(Collection::stream)
                .toList();
        Date startedAt = performanceResultDto.getCreatedAt();
        Date completedAt = new Date();
        long totalDurationMs = startedAt == null ? 0 : Math.max(0, completedAt.getTime() - startedAt.getTime());
        Map<Long, List<PerformanceResultItemDto>> stepResults = allSamples.stream()
                .collect(Collectors.groupingBy(PerformanceResultItemDto::getProcessFlowStepId));
        List<PerformanceSummary> stepSummaryList = PerformanceMetricsCalculator.buildStepSummaries(stepShortCodeMap, stepResults, totalDurationMs);
        PerformanceRunSummary runSummary = PerformanceMetricsCalculator.buildRunSummary(
                allSamples,
                startedAt,
                completedAt,
                Optional.ofNullable(performanceResultDto.getThreadCount()).orElse(resultMap.size()),
                Optional.ofNullable(performanceResultDto.getRampUpPeriod()).orElse(0),
                status
        );
        performanceResultItemRepository.updatePerformanceResults(performanceResultDto.getPerformanceResultId(), runningItem);
        PerfRsltEntity entity = performanceResultRepository.findById(performanceResultDto.getPerformanceResultId())
                .orElseThrow(() -> new RuntimeException("Performance result not found: " + performanceResultDto.getPerformanceResultId()));
        PerformanceThresholdConfig thresholdConfig = entity.getThresholdConfig();
        if (thresholdConfig == null) {
            throw new IllegalStateException("Threshold config is missing for performance result: " + performanceResultDto.getPerformanceResultId());
        }
        PerformanceThresholdResult thresholdResult = PerformanceThresholdEvaluator.evaluate(runSummary, thresholdConfig);
        GeneralEnums.PerformanceStatus finalStatus = status == GeneralEnums.PerformanceStatus.COMPLETED
                ? PerformanceThresholdEvaluator.finalStatus(thresholdResult)
                : status;
        PerformanceAnalysisSummary analysisSummary = withStopWarnings(
                PerformanceAnalysisBuilder.buildAnalysis(runSummary, stepSummaryList, thresholdResult),
                status,
                forceStopped
        );
        PerformanceErrorAnalysis errorAnalysis = PerformanceAnalysisBuilder.buildErrorAnalysis(runSummary, stepSummaryList, runningItem);
        PerformanceEnvironmentMetrics environmentMetrics = PerformanceAnalysisBuilder.unavailableEnvironmentMetrics();

        entity.setPerfStatus(finalStatus);
        entity.setSummary(stepSummaryList);
        entity.setRunSummary(runSummary);
        entity.setThresholdResult(thresholdResult);
        entity.setAnalysisSummary(analysisSummary);
        entity.setErrorAnalysis(errorAnalysis);
        entity.setEnvironmentMetrics(environmentMetrics);
        PerfRsltEntity saved = performanceResultRepository.save(entity);
        PerfRsltEntity compared = performanceBaselineService.applyAutomaticBaselineComparison(saved);
        compared.setValidationChecklist(performanceValidationChecklistBuilder.build(compared, runningItem));
        PerformanceReportSnapshotService.PerformanceReportSnapshot snapshot = performanceReportSnapshotService.build(compared, runningItem);
        compared.setInsightReport(snapshot.insightReport());
        compared.setAiManagementReport(snapshot.aiManagementReport());
        performanceResultRepository.save(compared);
    }

    private PerformanceAnalysisSummary withStopWarnings(PerformanceAnalysisSummary analysisSummary,
                                                        GeneralEnums.PerformanceStatus status,
                                                        boolean forceStopped) {
        if (status != GeneralEnums.PerformanceStatus.STOPPED) {
            return analysisSummary;
        }
        List<String> warnings = new ArrayList<>(Optional.ofNullable(analysisSummary.warnings()).orElse(List.of()));
        warnings.add(forceStopped ? "Test kullanıcı tarafından zorla durduruldu." : "Test kullanıcı tarafından durduruldu.");
        return new PerformanceAnalysisSummary(
                status,
                analysisSummary.thresholdResult(),
                analysisSummary.problemStepName(),
                analysisSummary.slowestStepName(),
                analysisSummary.highestP95StepName(),
                analysisSummary.highestP99StepName(),
                analysisSummary.highestErrorStepName(),
                analysisSummary.highestStdDeviationStepName(),
                analysisSummary.summaryText(),
                warnings
        );
    }

    private ResponseEntity<String> executeSqlQuery(ProcessFlowStepDto step, String systemShortCode, ApiInformationDto apiInformation, Map<String, String> parameterContext, Long projectId) {
        try {
            String sqlQuery = replaceSqlParameters(step.getPlIn(), parameterContext);

            if (StringUtils.isEmpty(sqlQuery)) {
                throw new RuntimeException("SQL query is empty in step.plIn");
            }

            //String processedSql = replaceSqlParameters(sqlQuery, parameterContext);

            Allure.addAttachment("SQL Query: " + step.getStepShortCode(), sqlQuery);

            Object result = databaseHelper.executeSql(sqlQuery,systemShortCode,projectId);

            String jsonResult = objectMapper.writeValueAsString(result);

            Allure.addAttachment("SQL Result: " + step.getStepShortCode(), jsonResult);

            return ResponseEntity.ok(jsonResult);
        } catch (Exception e) {
            String errorMessage = "SQL execution failed for step " + step.getStepShortCode() + ": " + e.getMessage();
            Allure.addAttachment("SQL Error: " + step.getStepShortCode(), errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
    }

    private String replaceSqlParameters(String sql, Map<String, String> parameterContext) {
        if (sql == null || !sql.contains("${")) {
            return sql;
        }
        if (parameterContext == null || parameterContext.isEmpty()) {
            return sql;
        }
        String result = sql;
        int startIndex = 0;

        while ((startIndex = result.indexOf("${", startIndex)) != -1) {
            int endIndex = result.indexOf("}", startIndex);
            if (endIndex != -1) {
                String key = result.substring(startIndex + 2, endIndex);
                String value = parameterContext.get(key);

                if (value != null) {
                    String escapedValue = value.replace("'", "''");
                    result = result.substring(0, startIndex) + "'" + escapedValue + "'" + result.substring(endIndex + 1);
                    startIndex += escapedValue.length() + 2;
                } else {
                    startIndex = endIndex + 1;
                }
            } else {
                break;
            }
        }
        return result;
    }

    //Darwin akışları için eklendi,parametreli koşumlar için kullanılıyor !!!!
    public ResponseEntity<Map<String, Object>> callXmlProcessFlowCallApi2(ProjectDto project, String systemShortCode, ProcessFlowDto processFlow, boolean fromTest, boolean continueOnError) throws JsonProcessingException {
        processFlow.setSystemShortCode(systemShortCode);
        Map<String, String> resultMap = new LinkedHashMap<>();
        Map<String, String> errorInfo = null;

        for (ProcessFlowStepDto step : processFlow.getProcessFlowStepList()) {
            ApiInformationDto apiInformation = step.getApiInformation();
            if (apiInformation.isTokenApi()) {
                String shortCode = project.getGeneralWebSystemDtoList().stream()
                        .filter(url -> Objects.nonNull(url.getBaseUrlShortCode())
                                && url.getBaseUrlShortCode().equals(systemShortCode))
                        .findAny()
                        .map(GeneralWebSystemDto::getShortCode)
                        .orElse(systemShortCode);

                apiInformation.setShortCode(shortCode);
            } else {
                apiInformation.setShortCode(systemShortCode);
            }

            this.processRequestBody(step, apiInformation, processFlow);
            ResponseEntity<String> result;
            if (apiInformation.isSqlQuery()) {
                result = executeSqlQuery(step, systemShortCode, apiInformation, processFlow.getParameterContext(), project.getProjectId());
            } else {
                result = this.sendApiInformationXML(apiInformation, step, fromTest, project.getProjectId(), processFlow.getParameterContext());
            }
            if (result.getStatusCode().is2xxSuccessful()) {
                if (step.getApiInformation().isTokenApi()) {
                    String bearerToken = "Bearer " + extractAccessTokenFromOAuthResponse(result.getBody());
                    processFlow.getGlobalHeaders().put("Authorization", bearerToken);
                } else {
                    processFlow.processHeaders(result, step.getHeaderExtractor());
                }
                processFlow.processParameters(result, step.getParameterExtractor());
                if (!apiInformation.isSqlQuery()) {
                    logApiCall(project.getName(), result.getStatusCode().value(),
                            this.systemEndpoint.find(apiInformation.getShortCode() + "_" + project.getProjectId()) + apiInformation.getSrvcName(),
                            apiInformation.getPlIn(), result.getBody());
                }
                resultMap.put(step.getStepShortCode(), StringUtils.deleteWhitespace(result.getBody()));
            } else {
                resultMap.put(step.getStepShortCode(), StringUtils.deleteWhitespace(result.getBody()));
                errorInfo = handleErrorResponse(step, result, apiInformation, project);
                if (!continueOnError) break;
            }
        }
        return buildFinalResponseForDarwin(resultMap, processFlow.getParameterContext(), errorInfo);
    }

    public ResponseEntity<Map<String, Object>> parallelCallXmlProcessFlowWithParameterContext(String project, String system,
                                                                                             String flow, ParallelCallRequestDto parallelCallRequestDto) {
        int threadCount = Math.min(5, parallelCallRequestDto.getTotalCalls());
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();
        
        for (int i = 0; i < parallelCallRequestDto.getTotalCalls(); i++) {
            final int callIndex = i + 1;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    ResponseEntity<Map<String, Object>> response =
                            callXmlProcessFlowWithParameterContext(project, system, flow, false, false,
                                    parallelCallRequestDto.getParameterRequest());

                    Map<String, Object> responseBody = Optional.ofNullable(response.getBody())
                            .orElse(new LinkedHashMap<>());

                    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
                    @SuppressWarnings("unchecked")
                    Optional<Map<String, Object>> parametrelerOpt = Optional.ofNullable(responseBody.get("Parametreler"))
                            .filter(value -> value instanceof Map)
                            .map(value -> (Map<String, Object>) value);

                    parametrelerOpt.flatMap(parametrelerMap -> Optional.ofNullable(parametrelerMap.get("customerId")))
                            .ifPresent(customerId -> result.put(callIndex + "-customerId", customerId));

                    /*Optional<Object> firstValue = responseBody.entrySet().stream()
                            .findFirst()
                            .map(Map.Entry::getValue);
                    if (firstValue.isPresent() && firstValue.get() instanceof Map) {
                        Map<String, String> innerMap = (Map<String, String>) firstValue.get();
                        innerMap.entrySet().stream()
                                .reduce((first, second) -> second)
                                .ifPresent(lastEntry -> result.put(lastEntry.getKey(), lastEntry.getValue()));
                    }
                    responseBody.entrySet().stream()
                            .reduce((first, second) -> second)
                            .ifPresent(lastEntry -> result.put(lastEntry.getKey(), lastEntry.getValue()));*/

                    return result;
                } catch (Exception ex) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", ex.getMessage());
                    return errorResult;
                }
            }, executor));
        }

        Map<String, Object> results = new LinkedHashMap<>();
        futures.forEach(future -> results.putAll(future.join()));

        executor.shutdown();
        return ResponseEntity.ok(results);
    }
}
