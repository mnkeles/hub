package etiya.omniAutomation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import etiya.omniAutomation.business.dto.ApiInformationDto;
import etiya.omniAutomation.business.dto.GeneralWebSystemDto;
import etiya.omniAutomation.business.dto.HarAnalysisResultDto;
import etiya.omniAutomation.business.dto.ProjectDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class HarAnalysisService {

    private static final Set<String> IGNORED_FILE_EXTENSIONS = Set.of(
            "css",
            "js",
            "mjs",
            "map",
            "png",
            "jpg",
            "jpeg",
            "gif",
            "svg",
            "ico",
            "woff",
            "woff2",
            "ttf",
            "eot",
            "otf",
            "webp",
            "bmp"
    );
    private static final Set<String> NOISY_PATH_SEGMENTS = Set.of(
            "swagger",
            "openapi",
            "favicon.ico",
            "sockjs",
            "websocket"
    );
    private static final Set<String> GENERIC_FLOW_SEGMENTS = Set.of(
            "api",
            "rest",
            "services",
            "service",
            "public",
            "private",
            "v1",
            "v2",
            "v3"
    );
    private static final Pattern NUMERIC_SEGMENT_PATTERN = Pattern.compile("/\\d+(?=/|$)");
    private static final Pattern UUID_SEGMENT_PATTERN = Pattern.compile("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");
    private static final Pattern LONG_TOKEN_SEGMENT_PATTERN = Pattern.compile("/[A-Za-z0-9_-]{18,}(?=/|$)");
    private static final int MAX_REQUEST_BODY_SAMPLE_CHARS = 16 * 1024;
    private static final int MAX_RESPONSE_BODY_SAMPLE_CHARS = 4 * 1024;
    private static final int MAX_HEADER_VALUE_SAMPLE_CHARS = 512;
    private static final int MAX_PARAMETER_VALUE_SAMPLE_CHARS = 2 * 1024;
    private static final int MAX_STEP_PARAMETER_COUNT = 50;
    private static final Set<String> SENSITIVE_HEADERS = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "api-key",
            "proxy-authorization"
    );

    private final ObjectMapper objectMapper;
    private final ProjectService projectService;
    private final ApiInformationServiceImpl apiInformationService;

    public HarAnalysisService(
            ObjectMapper objectMapper,
            ProjectService projectService,
            ApiInformationServiceImpl apiInformationService
    ) {
        this.objectMapper = objectMapper;
        this.projectService = projectService;
        this.apiInformationService = apiInformationService;
    }

    public HarAnalysisResultDto analyze(String projectShortCode, String systemShortCode, String harContent) {
        String normalizedProjectShortCode = normalizeShortCode(projectShortCode);
        String normalizedSystemShortCode = normalizeShortCode(systemShortCode);
        validateRequest(normalizedProjectShortCode, normalizedSystemShortCode, harContent);
        return analyzeLocally(normalizedProjectShortCode, normalizedSystemShortCode, harContent);
    }

    public HarAnalysisResultDto analyzeUpload(String projectShortCode, String systemShortCode, MultipartFile file) {
        String normalizedProjectShortCode = normalizeShortCode(projectShortCode);
        String normalizedSystemShortCode = normalizeShortCode(systemShortCode);
        validateRequiredShortCodes(normalizedProjectShortCode, normalizedSystemShortCode);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("HAR file is required.");
        }

        try {
            String harContent = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return analyzeLocally(normalizedProjectShortCode, normalizedSystemShortCode, harContent);
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("HAR file could not be analyzed locally.", ex);
        }
    }

    private HarAnalysisResultDto analyzeLocally(String projectShortCode, String systemShortCode, String harContent) {
        ProjectDto project = requireProject(projectShortCode);
        JsonNode entriesNode = parseEntries(harContent);
        Set<String> requestedSystemBaseUrls = resolveRequestedSystemBaseUrls(project, systemShortCode);
        Set<String> availableSystemCodes = resolveAvailableSystemCodes(project);
        List<ApiInformationDto> projectApis = resolveProjectApis(projectShortCode);

        List<ProcessedRequest> processedRequests = new ArrayList<>();
        List<Map<String, Object>> ignoredRequests = new ArrayList<>();
        LinkedHashSet<String> discoveredBaseUrls = new LinkedHashSet<>();
        LinkedHashMap<EndpointKey, EndpointAggregate> aggregateMap = new LinkedHashMap<>();

        int totalEntries = 0;
        int sourceEntryIndex = 0;
        for (JsonNode entryNode : entriesNode) {
            totalEntries++;
            sourceEntryIndex++;
            int currentSourceEntryIndex = sourceEntryIndex;

            JsonNode requestNode = entryNode.path("request");
            String requestUrl = StringUtils.trimToNull(requestNode.path("url").asText(null));
            String method = StringUtils.upperCase(StringUtils.defaultIfBlank(requestNode.path("method").asText(null), "GET"), Locale.ROOT);
            if (requestUrl == null) {
                ignoredRequests.add(buildIgnoredRequest(currentSourceEntryIndex, method, null, null, "İstek URL'i bulunamadı."));
                continue;
            }

            URI requestUri = toUri(requestUrl);
            if (requestUri == null || StringUtils.isBlank(requestUri.getScheme()) || StringUtils.isBlank(requestUri.getHost())) {
                ignoredRequests.add(buildIgnoredRequest(currentSourceEntryIndex, method, requestUrl, null, "Geçersiz veya göreli request URL olduğu için analize alınmadı."));
                continue;
            }

            String path = StringUtils.defaultIfBlank(requestUri.getPath(), "/");
            String mimeType = extractMimeType(entryNode);
            String ignoreReason = resolveIgnoreReason(method, requestUri, path, mimeType);
            if (ignoreReason != null) {
                ignoredRequests.add(buildIgnoredRequest(currentSourceEntryIndex, method, requestUrl, path, ignoreReason));
                continue;
            }

            String baseUrl = normalizeBaseUrl(requestUri);
            String normalizedPath = normalizePath(path);
            RequestCapture requestCapture = buildRequestCapture(requestNode, requestUri, path, normalizedPath);
            ResponseCapture responseCapture = buildResponseCapture(entryNode);
            List<String> queryParameterNames = requestCapture.queryParameters().isEmpty()
                    ? extractQueryParameterNames(requestUri)
                    : new ArrayList<>(requestCapture.queryParameters().keySet());
            long durationMs = entryNode.path("time").asLong(0L);
            int status = entryNode.path("response").path("status").asInt(0);
            boolean matchesRequestedSystem = requestedSystemBaseUrls.isEmpty() || requestedSystemBaseUrls.contains(baseUrl);

            EndpointKey endpointKey = new EndpointKey(method, baseUrl, normalizedPath);
            EndpointAggregate aggregate = aggregateMap.computeIfAbsent(
                    endpointKey,
                    key -> new EndpointAggregate(method, baseUrl, path, normalizedPath, currentSourceEntryIndex)
            );
            aggregate.recordOccurrence(status, mimeType, durationMs, queryParameterNames, currentSourceEntryIndex, matchesRequestedSystem, requestCapture, responseCapture);
            processedRequests.add(new ProcessedRequest(
                    currentSourceEntryIndex,
                    endpointKey,
                    method,
                    baseUrl,
                    path,
                    normalizedPath,
                    status,
                    durationMs
            ));
            discoveredBaseUrls.add(baseUrl);
        }

        List<EndpointAggregate> endpointAggregates = aggregateMap.values().stream()
                .sorted(Comparator.comparingInt(EndpointAggregate::firstEntryIndex))
                .toList();

        List<Map<String, Object>> endpoints = buildEndpoints(endpointAggregates);
        List<Map<String, Object>> logicalSteps = buildLogicalSteps(endpointAggregates, projectApis);
        Map<EndpointKey, Integer> stepOrderByEndpoint = new LinkedHashMap<>();
        int stepOrder = 1;
        for (EndpointAggregate aggregate : endpointAggregates) {
            stepOrderByEndpoint.put(new EndpointKey(aggregate.method(), aggregate.baseUrl(), aggregate.normalizedPath()), stepOrder++);
        }

        List<Map<String, Object>> relationships = buildRelationships(processedRequests, logicalSteps, stepOrderByEndpoint);
        String flowShortCodeSuggestion = buildFlowShortCodeSuggestion(projectShortCode, systemShortCode, logicalSteps, endpointAggregates);
        String summary = buildSummary(processedRequests, endpointAggregates, discoveredBaseUrls, flowShortCodeSuggestion);
        List<String> warnings = buildWarnings(
                processedRequests,
                ignoredRequests,
                discoveredBaseUrls,
                endpointAggregates,
                requestedSystemBaseUrls,
                availableSystemCodes,
                systemShortCode
        );
        List<String> recommendations = buildRecommendations(processedRequests, endpointAggregates, ignoredRequests, discoveredBaseUrls, warnings);
        Map<String, Object> statistics = buildStatistics(totalEntries, processedRequests, ignoredRequests, endpointAggregates, discoveredBaseUrls, requestedSystemBaseUrls);
        statistics.put("logicalSteps", logicalSteps.size());
        statistics.put("matchedSteps", logicalSteps.stream().filter(this::hasMatchedApiInformation).count());
        statistics.put("unmatchedSteps", logicalSteps.stream().filter(step -> !hasMatchedApiInformation(step)).count());
        Map<String, Object> processFlowDraft = buildProcessFlowDraft(
                project,
                projectShortCode,
                systemShortCode,
                flowShortCodeSuggestion,
                summary,
                logicalSteps,
                relationships
        );

        HarAnalysisResultDto analysisResult = new HarAnalysisResultDto();
        analysisResult.setProjectShortCode(projectShortCode);
        analysisResult.setSystemShortCode(systemShortCode);
        analysisResult.setProjectId(project.getProjectId());
        analysisResult.setProjectName(project.getName());
        analysisResult.setFlowShortCodeSuggestion(flowShortCodeSuggestion);
        analysisResult.setSummary(summary);
        analysisResult.setEndpoints(endpoints);
        analysisResult.setBaseUrls(new ArrayList<>(discoveredBaseUrls));
        analysisResult.setRecommendations(recommendations);
        analysisResult.setWarnings(warnings);
        analysisResult.setStatistics(statistics);
        analysisResult.setIgnoredRequests(ignoredRequests);
        analysisResult.setLogicalSteps(logicalSteps);
        analysisResult.setRelationships(relationships);
        analysisResult.setProcessFlowDraft(processFlowDraft);
        return analysisResult;
    }

    private void validateRequest(String projectShortCode, String systemShortCode, String harContent) {
        validateRequiredShortCodes(projectShortCode, systemShortCode);
        if (StringUtils.isBlank(harContent)) {
            throw new IllegalArgumentException("HAR içeriği boş olamaz.");
        }
    }

    private void validateRequiredShortCodes(String projectShortCode, String systemShortCode) {
        if (StringUtils.isBlank(projectShortCode)) {
            throw new IllegalArgumentException("projectShortCode boş olamaz.");
        }
        if (StringUtils.isBlank(systemShortCode)) {
            throw new IllegalArgumentException("systemShortCode boş olamaz.");
        }
    }

    private ProjectDto requireProject(String projectShortCode) {
        ProjectDto project = projectService.getProject(projectShortCode);
        if (project == null) {
            throw new IllegalArgumentException("Project bulunamadı: " + projectShortCode);
        }
        return project;
    }

    private JsonNode parseEntries(String harContent) {
        try {
            JsonNode rootNode = objectMapper.readTree(harContent);
            JsonNode entriesNode = rootNode.path("log").path("entries");
            if (!entriesNode.isArray()) {
                throw new IllegalArgumentException("HAR içeriğinde log.entries dizisi bulunamadı.");
            }
            return entriesNode;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("HAR içeriği parse edilemedi.", ex);
        }
    }

    private String normalizeShortCode(String shortCode) {
        return StringUtils.upperCase(StringUtils.trimToNull(shortCode), Locale.ROOT);
    }

    private Set<String> resolveRequestedSystemBaseUrls(ProjectDto project, String systemShortCode) {
        if (project == null || project.getGeneralWebSystemDtoList() == null || StringUtils.isBlank(systemShortCode)) {
            return Set.of();
        }

        return project.getGeneralWebSystemDtoList().stream()
                .filter(Objects::nonNull)
                .filter(system -> StringUtils.equalsIgnoreCase(systemShortCode, system.getShortCode())
                        || StringUtils.equalsIgnoreCase(systemShortCode, system.getBaseUrlShortCode()))
                .map(GeneralWebSystemDto::getUrl)
                .filter(StringUtils::isNotBlank)
                .map(this::normalizeBaseUrl)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> resolveAvailableSystemCodes(ProjectDto project) {
        if (project == null || project.getGeneralWebSystemDtoList() == null) {
            return Set.of();
        }

        LinkedHashSet<String> systemCodes = new LinkedHashSet<>();
        for (GeneralWebSystemDto generalWebSystem : project.getGeneralWebSystemDtoList()) {
            if (generalWebSystem == null) {
                continue;
            }
            if (StringUtils.isNotBlank(generalWebSystem.getBaseUrlShortCode())) {
                systemCodes.add(StringUtils.upperCase(generalWebSystem.getBaseUrlShortCode(), Locale.ROOT));
            }
            if (StringUtils.isNotBlank(generalWebSystem.getShortCode())) {
                systemCodes.add(StringUtils.upperCase(generalWebSystem.getShortCode(), Locale.ROOT));
            }
        }
        return systemCodes;
    }

    private URI toUri(String requestUrl) {
        try {
            return new URI(StringUtils.trimToEmpty(requestUrl).replace(" ", "%20"));
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String resolveIgnoreReason(String method, URI requestUri, String path, String mimeType) {
        if (!StringUtils.equalsAnyIgnoreCase(requestUri.getScheme(), "http", "https")) {
            return "HTTP/HTTPS dışı istek olduğu için analize alınmadı.";
        }
        if (StringUtils.equalsIgnoreCase(method, "OPTIONS")) {
            return "Preflight OPTIONS isteği olduğu için analize alınmadı.";
        }

        String extension = extractPathExtension(path);
        if (StringUtils.isNotBlank(extension) && IGNORED_FILE_EXTENSIONS.contains(StringUtils.lowerCase(extension, Locale.ROOT))) {
            return "Statik asset isteği olduğu için analize alınmadı.";
        }

        String normalizedPath = StringUtils.lowerCase(StringUtils.defaultString(path), Locale.ROOT);
        if (NOISY_PATH_SEGMENTS.stream().anyMatch(normalizedPath::contains)) {
            return "Dokümantasyon, websocket veya favicon isteği olduğu için analize alınmadı.";
        }

        if (StringUtils.containsIgnoreCase(mimeType, "text/css")
                || StringUtils.containsIgnoreCase(mimeType, "javascript")
                || StringUtils.containsIgnoreCase(mimeType, "image/")) {
            return "API dışı statik içerik cevabı olduğu için analize alınmadı.";
        }

        return null;
    }

    private String extractMimeType(JsonNode entryNode) {
        String mimeType = StringUtils.trimToNull(entryNode.path("response").path("content").path("mimeType").asText(null));
        if (mimeType != null) {
            return mimeType;
        }

        for (JsonNode headerNode : entryNode.path("response").path("headers")) {
            String headerName = StringUtils.trimToNull(headerNode.path("name").asText(null));
            if (StringUtils.equalsIgnoreCase(headerName, "Content-Type")) {
                return StringUtils.trimToNull(headerNode.path("value").asText(null));
            }
        }
        return null;
    }

    private String extractPathExtension(String path) {
        String fileName = StringUtils.substringAfterLast(StringUtils.defaultString(path), "/");
        if (StringUtils.isBlank(fileName) || !fileName.contains(".")) {
            return null;
        }
        return StringUtils.substringAfterLast(fileName, ".");
    }

    private List<String> extractQueryParameterNames(URI requestUri) {
        String query = requestUri.getQuery();
        if (StringUtils.isBlank(query)) {
            return List.of();
        }

        return Arrays.stream(query.split("&"))
                .map(item -> StringUtils.substringBefore(item, "="))
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private RequestCapture buildRequestCapture(JsonNode requestNode, URI requestUri, String rawPath, String normalizedPath) {
        LinkedHashMap<String, String> queryParameters = extractQueryParameters(requestNode, requestUri);
        List<Map<String, Object>> stepParameters = new ArrayList<>();
        for (Map.Entry<String, String> queryParameter : queryParameters.entrySet()) {
            addStepParameter(stepParameters, queryParameter.getKey(), queryParameter.getValue());
        }
        String servicePathTemplate = buildServicePathTemplate(rawPath, normalizedPath, queryParameters, stepParameters);

        JsonNode postDataNode = requestNode.path("postData");
        String requestBody = StringUtils.trimToNull(postDataNode.path("text").asText(null));
        String requestMimeType = StringUtils.trimToNull(postDataNode.path("mimeType").asText(null));
        String requestPayloadTemplate = buildRequestPayloadTemplate(requestBody, requestMimeType, stepParameters);
        String requestBodySample = capText(requestBody, MAX_REQUEST_BODY_SAMPLE_CHARS);
        long requestSizeBytes = postDataNode.path("bodySize").asLong(-1L);
        if (requestSizeBytes < 0L && requestBody != null) {
            requestSizeBytes = requestBody.getBytes(StandardCharsets.UTF_8).length;
        }

        return new RequestCapture(
                queryParameters,
                extractHeaders(requestNode.path("headers")),
                requestBodySample,
                isTruncated(requestBody, MAX_REQUEST_BODY_SAMPLE_CHARS),
                requestSizeBytes < 0L ? null : requestSizeBytes,
                requestPayloadTemplate,
                servicePathTemplate,
                stepParameters
        );
    }

    private ResponseCapture buildResponseCapture(JsonNode entryNode) {
        JsonNode responseNode = entryNode.path("response");
        JsonNode contentNode = responseNode.path("content");
        String encoding = StringUtils.trimToNull(contentNode.path("encoding").asText(null));
        String responseBody = StringUtils.equalsIgnoreCase(encoding, "base64")
                ? null
                : StringUtils.trimToNull(contentNode.path("text").asText(null));
        String responseBodySample = capText(responseBody, MAX_RESPONSE_BODY_SAMPLE_CHARS);
        long responseSizeBytes = contentNode.path("size").asLong(-1L);
        if (responseSizeBytes < 0L && responseBody != null) {
            responseSizeBytes = responseBody.getBytes(StandardCharsets.UTF_8).length;
        }

        return new ResponseCapture(
                extractHeaders(responseNode.path("headers")),
                responseBodySample,
                isTruncated(responseBody, MAX_RESPONSE_BODY_SAMPLE_CHARS),
                responseSizeBytes < 0L ? null : responseSizeBytes
        );
    }

    private LinkedHashMap<String, String> extractQueryParameters(JsonNode requestNode, URI requestUri) {
        LinkedHashMap<String, String> queryParameters = new LinkedHashMap<>();
        JsonNode queryStringNode = requestNode.path("queryString");
        if (queryStringNode.isArray()) {
            for (JsonNode queryParameterNode : queryStringNode) {
                String name = StringUtils.trimToNull(queryParameterNode.path("name").asText(null));
                if (name == null || queryParameters.containsKey(name)) {
                    continue;
                }
                queryParameters.put(name, capText(queryParameterNode.path("value").asText(""), MAX_PARAMETER_VALUE_SAMPLE_CHARS));
            }
        }

        if (!queryParameters.isEmpty() || StringUtils.isBlank(requestUri.getRawQuery())) {
            return queryParameters;
        }

        for (String queryItem : requestUri.getRawQuery().split("&")) {
            String name = decodeUrlComponent(StringUtils.substringBefore(queryItem, "="));
            if (StringUtils.isBlank(name) || queryParameters.containsKey(name)) {
                continue;
            }
            String value = queryItem.contains("=") ? decodeUrlComponent(StringUtils.substringAfter(queryItem, "=")) : "";
            queryParameters.put(name, capText(value, MAX_PARAMETER_VALUE_SAMPLE_CHARS));
        }
        return queryParameters;
    }

    private LinkedHashMap<String, String> extractHeaders(JsonNode headersNode) {
        LinkedHashMap<String, String> headers = new LinkedHashMap<>();
        if (!headersNode.isArray()) {
            return headers;
        }

        for (JsonNode headerNode : headersNode) {
            String name = StringUtils.trimToNull(headerNode.path("name").asText(null));
            if (name == null || headers.containsKey(name)) {
                continue;
            }

            String normalizedHeaderName = StringUtils.lowerCase(name, Locale.ROOT);
            String value = SENSITIVE_HEADERS.contains(normalizedHeaderName)
                    ? "[REDACTED]"
                    : capText(headerNode.path("value").asText(""), MAX_HEADER_VALUE_SAMPLE_CHARS);
            headers.put(name, value);
        }
        return headers;
    }

    private String buildServicePathTemplate(
            String rawPath,
            String normalizedPath,
            LinkedHashMap<String, String> queryParameters,
            List<Map<String, Object>> stepParameters
    ) {
        String servicePathTemplate = StringUtils.defaultIfBlank(normalizedPath, rawPath);
        List<String> rawPathSegments = Arrays.stream(StringUtils.defaultString(rawPath).split("/"))
                .filter(StringUtils::isNotBlank)
                .toList();
        List<String> normalizedPathSegments = Arrays.stream(StringUtils.defaultString(normalizedPath).split("/"))
                .filter(StringUtils::isNotBlank)
                .toList();

        if (rawPathSegments.size() == normalizedPathSegments.size()) {
            for (int index = 0; index < rawPathSegments.size(); index++) {
                String normalizedSegment = normalizedPathSegments.get(index);
                if (!StringUtils.equalsAny(normalizedSegment, "{id}", "{token}")) {
                    continue;
                }

                String parameterName = sanitizeParameterName(resolvePathParameterName(rawPathSegments, index, normalizedSegment));
                addStepParameter(stepParameters, parameterName, rawPathSegments.get(index));
                servicePathTemplate = StringUtils.replaceOnce(servicePathTemplate, normalizedSegment, "${" + parameterName + "}");
            }
        }

        if (!queryParameters.isEmpty()) {
            String queryTemplate = queryParameters.keySet().stream()
                    .map(name -> name + "=${" + sanitizeParameterName(name) + "}")
                    .collect(Collectors.joining("&"));
            servicePathTemplate += "?" + queryTemplate;
        }

        return servicePathTemplate;
    }

    private String resolvePathParameterName(List<String> rawPathSegments, int index, String normalizedSegment) {
        String previousSegment = index > 0 ? normalizeSegmentForShortCode(rawPathSegments.get(index - 1)) : null;
        String suffix = StringUtils.equals(normalizedSegment, "{token}") ? "token" : "id";
        return StringUtils.isBlank(previousSegment) ? suffix : previousSegment + "_" + suffix;
    }

    private String buildRequestPayloadTemplate(String requestBody, String requestMimeType, List<Map<String, Object>> stepParameters) {
        if (StringUtils.isBlank(requestBody)) {
            return "";
        }

        String cappedRequestBody = capText(requestBody, MAX_REQUEST_BODY_SAMPLE_CHARS);
        if (isTruncated(requestBody, MAX_REQUEST_BODY_SAMPLE_CHARS)) {
            return cappedRequestBody;
        }

        if (StringUtils.containsIgnoreCase(requestMimeType, "json") || looksLikeJson(requestBody)) {
            try {
                JsonNode templateNode = toTemplateJsonNode(objectMapper.readTree(requestBody), "", stepParameters);
                return objectMapper.writeValueAsString(templateNode);
            } catch (Exception ignored) {
                return cappedRequestBody;
            }
        }

        if (StringUtils.containsIgnoreCase(requestMimeType, "x-www-form-urlencoded")) {
            return buildFormUrlEncodedTemplate(requestBody, stepParameters);
        }

        return cappedRequestBody;
    }

    private JsonNode toTemplateJsonNode(JsonNode node, String pathPrefix, List<Map<String, Object>> stepParameters) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return node;
        }
        if (node.isObject()) {
            ObjectNode objectNode = objectMapper.createObjectNode();
            node.fields().forEachRemaining(entry -> objectNode.set(
                    entry.getKey(),
                    toTemplateJsonNode(entry.getValue(), appendPath(pathPrefix, entry.getKey()), stepParameters)
            ));
            return objectNode;
        }
        if (node.isArray()) {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (int index = 0; index < node.size(); index++) {
                arrayNode.add(toTemplateJsonNode(node.get(index), appendPath(pathPrefix, String.valueOf(index)), stepParameters));
            }
            return arrayNode;
        }
        if (node.isValueNode() && stepParameters.size() < MAX_STEP_PARAMETER_COUNT) {
            String parameterName = sanitizeParameterName(pathPrefix);
            String parameterPlaceholder = "${" + parameterName + "}";
            addStepParameter(stepParameters, parameterName, node.isTextual() ? node.asText() : node.asText(""));
            return new TextNode(parameterPlaceholder);
        }
        return node;
    }

    private String buildFormUrlEncodedTemplate(String requestBody, List<Map<String, Object>> stepParameters) {
        List<String> templateItems = new ArrayList<>();
        for (String item : requestBody.split("&")) {
            String rawName = StringUtils.substringBefore(item, "=");
            String name = decodeUrlComponent(rawName);
            if (StringUtils.isBlank(name)) {
                templateItems.add(item);
                continue;
            }
            String parameterName = sanitizeParameterName(name);
            String rawValue = item.contains("=") ? StringUtils.substringAfter(item, "=") : "";
            addStepParameter(stepParameters, parameterName, decodeUrlComponent(rawValue));
            templateItems.add(rawName + "=${" + parameterName + "}");
        }
        return String.join("&", templateItems);
    }

    private void addStepParameter(List<Map<String, Object>> stepParameters, String rawName, String rawValue) {
        if (StringUtils.isBlank(rawName) || stepParameters.size() >= MAX_STEP_PARAMETER_COUNT) {
            return;
        }
        String parameterName = sanitizeParameterName(rawName);
        String shortCode = "${" + parameterName + "}";
        boolean alreadyAdded = stepParameters.stream()
                .anyMatch(parameter -> StringUtils.equals(shortCode, Objects.toString(parameter.get("shortCode"), null)));
        if (alreadyAdded) {
            return;
        }

        LinkedHashMap<String, Object> parameter = new LinkedHashMap<>();
        parameter.put("processFlowStepParmId", null);
        parameter.put("shortCode", shortCode);
        parameter.put("value", capText(rawValue, MAX_PARAMETER_VALUE_SAMPLE_CHARS));
        parameter.put("valExpression", null);
        parameter.put("paramOrder", stepParameters.size() + 1);
        parameter.put("useContext", false);
        stepParameters.add(parameter);
    }

    private String appendPath(String pathPrefix, String key) {
        return StringUtils.isBlank(pathPrefix) ? key : pathPrefix + "_" + key;
    }

    private String sanitizeParameterName(String rawName) {
        String parameterName = StringUtils.defaultString(rawName).replaceAll("[^A-Za-z0-9_]+", "_");
        parameterName = parameterName.replaceAll("_+", "_");
        parameterName = StringUtils.strip(parameterName, "_");
        if (StringUtils.isBlank(parameterName)) {
            return "param_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        }
        return StringUtils.uncapitalize(parameterName);
    }

    private boolean looksLikeJson(String value) {
        String trimmedValue = StringUtils.trimToEmpty(value);
        return (trimmedValue.startsWith("{") && trimmedValue.endsWith("}"))
                || (trimmedValue.startsWith("[") && trimmedValue.endsWith("]"));
    }

    private String capText(String value, int maxChars) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxChars ? value : value.substring(0, maxChars);
    }

    private boolean isTruncated(String value, int maxChars) {
        return value != null && value.length() > maxChars;
    }

    private String decodeUrlComponent(String value) {
        if (value == null) {
            return "";
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return value;
        }
    }

    private String normalizeBaseUrl(String rawUrl) {
        URI uri = toUri(rawUrl);
        if (uri == null || StringUtils.isBlank(uri.getScheme()) || StringUtils.isBlank(uri.getHost())) {
            return StringUtils.trimToEmpty(rawUrl);
        }
        return normalizeBaseUrl(uri);
    }

    private String normalizeBaseUrl(URI uri) {
        StringBuilder builder = new StringBuilder();
        builder.append(StringUtils.lowerCase(uri.getScheme(), Locale.ROOT)).append("://").append(StringUtils.lowerCase(uri.getHost(), Locale.ROOT));
        if (uri.getPort() > -1) {
            builder.append(":").append(uri.getPort());
        }
        return builder.toString();
    }

    private String normalizePath(String rawPath) {
        String normalizedPath = StringUtils.defaultIfBlank(rawPath, "/").replaceAll("/+", "/");
        normalizedPath = NUMERIC_SEGMENT_PATTERN.matcher(normalizedPath).replaceAll("/{id}");
        normalizedPath = UUID_SEGMENT_PATTERN.matcher(normalizedPath).replaceAll("/{id}");
        normalizedPath = LONG_TOKEN_SEGMENT_PATTERN.matcher(normalizedPath).replaceAll("/{token}");
        return normalizedPath;
    }

    private List<ApiInformationDto> resolveProjectApis(String projectShortCode) {
        List<ApiInformationDto> apiInformations = apiInformationService.findAllByProjectShortCode(projectShortCode);
        if (apiInformations == null) {
            return List.of();
        }

        return apiInformations.stream()
                .filter(Objects::nonNull)
                .filter(apiInformation -> StringUtils.isNotBlank(resolveApiPath(apiInformation)))
                .toList();
    }

    private String resolveApiPath(ApiInformationDto apiInformation) {
        if (apiInformation == null) {
            return null;
        }

        return StringUtils.trimToNull(apiInformation.getSrvcName());
    }

    private String extractPathComponent(String rawPathOrUrl) {
        String trimmedValue = StringUtils.trimToNull(rawPathOrUrl);
        if (trimmedValue == null) {
            return null;
        }

        URI uri = toUri(trimmedValue);
        if (uri != null && StringUtils.isNotBlank(uri.getPath())) {
            return StringUtils.defaultIfBlank(uri.getPath(), "/");
        }

        return StringUtils.substringBefore(trimmedValue, "?");
    }

    private String normalizeComparableRawPath(String rawPathOrUrl) {
        String normalizedPath = StringUtils.defaultIfBlank(extractPathComponent(rawPathOrUrl), "/").replaceAll("/+", "/");
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = StringUtils.removeEnd(normalizedPath, "/");
        }
        return StringUtils.lowerCase(normalizedPath, Locale.ROOT);
    }

    private String normalizeComparablePath(String rawPathOrUrl) {
        return StringUtils.lowerCase(normalizePath(normalizeComparableRawPath(rawPathOrUrl)), Locale.ROOT);
    }

    private List<ApiMatchCandidate> buildApiMatchCandidates(EndpointAggregate aggregate, List<ApiInformationDto> projectApis) {
        String aggregateRawPath = normalizeComparableRawPath(aggregate.path());
        String aggregateNormalizedPath = normalizeComparablePath(aggregate.normalizedPath());
        List<ApiMatchCandidate> candidates = new ArrayList<>();

        for (ApiInformationDto apiInformation : projectApis) {
            String apiPath = resolveApiPath(apiInformation);
            if (StringUtils.isBlank(apiPath)) {
                continue;
            }

            String apiRawPath = normalizeComparableRawPath(apiPath);
            String apiNormalizedPath = normalizeComparablePath(apiPath);
            boolean rawPathMatches = StringUtils.equals(aggregateRawPath, apiRawPath);
            boolean normalizedPathMatches = StringUtils.equals(aggregateNormalizedPath, apiNormalizedPath);
            if (!rawPathMatches && !normalizedPathMatches) {
                continue;
            }

            String apiMethod = StringUtils.upperCase(StringUtils.trimToNull(apiInformation.getHttpMethod()), Locale.ROOT);
            boolean methodMatches = StringUtils.equals(aggregate.method(), apiMethod);
            boolean methodMissing = StringUtils.isBlank(apiMethod);
            int score = 0;
            List<String> reasons = new ArrayList<>();

            if (rawPathMatches) {
                score += 80;
                reasons.add("HAR path ile kayıtlı API URL'i birebir eşleşiyor.");
            }
            if (normalizedPathMatches) {
                score += rawPathMatches ? 15 : 90;
                reasons.add(rawPathMatches
                        ? "Normalize edilmiş path ile eşleşme doğrulandı."
                        : "HAR path ile kayıtlı API URL'i normalize edilince eşleşiyor.");
            }
            if (methodMatches) {
                score += 15;
                reasons.add("HTTP method eşleşiyor: " + aggregate.method());
            } else if (methodMissing) {
                score += 5;
                reasons.add("API kaydında HTTP method boş olduğu için path bazlı eşleşme kullanıldı.");
            } else {
                score -= 20;
                reasons.add("HTTP method farklı: HAR " + aggregate.method() + ", kayıt " + apiMethod);
            }

            boolean autoMatch = normalizedPathMatches && (methodMatches || methodMissing);
            candidates.add(new ApiMatchCandidate(apiInformation, score, autoMatch, reasons));
        }

        candidates.sort(
                Comparator.<ApiMatchCandidate>comparingInt(ApiMatchCandidate::score)
                        .reversed()
                        .thenComparing(candidate -> StringUtils.defaultString(candidate.apiInformation().getApiShortCode()))
                        .thenComparing(candidate -> StringUtils.defaultString(candidate.apiInformation().getShortCode()))
                        .thenComparingLong(candidate -> candidate.apiInformation().getId() == null ? Long.MAX_VALUE : candidate.apiInformation().getId())
        );
        return candidates;
    }

    private Map<String, Object> toApiMatch(ApiMatchCandidate candidate) {
        ApiInformationDto apiInformation = candidate.apiInformation();
        LinkedHashMap<String, Object> match = new LinkedHashMap<>();
        match.put("id", apiInformation.getId());
        match.put("name", apiInformation.getName());
        match.put("shortCode", apiInformation.getShortCode());
        match.put("apiShortCode", apiInformation.getApiShortCode());
        match.put("srvcName", apiInformation.getSrvcName());
        match.put("url", apiInformation.getSrvcName());
        match.put("httpMethod", apiInformation.getHttpMethod());
        match.put("method", apiInformation.getHttpMethod());
        match.put("statusCode", apiInformation.getStatusCode());
        match.put("projectId", apiInformation.getProjectId());
        match.put("providerSystemId", apiInformation.getProviderSystemId());
        match.put("score", candidate.score());
        match.put("matchScore", candidate.score());
        match.put("confidence", candidate.score());
        match.put("reasons", new ArrayList<>(candidate.reasons()));
        match.put("explanation", String.join(" ", candidate.reasons()));
        return match;
    }

    private Map<String, Object> buildApiInformationDraft(EndpointAggregate aggregate, String stepShortCodeSuggestion) {
        LinkedHashMap<String, Object> apiInformationDraft = new LinkedHashMap<>();
        apiInformationDraft.put("name", aggregate.method() + " " + aggregate.normalizedPath());
        apiInformationDraft.put("shortCode", stepShortCodeSuggestion);
        apiInformationDraft.put("apiShortCode", stepShortCodeSuggestion);
        apiInformationDraft.put("srvcName", aggregate.servicePathTemplate());
        apiInformationDraft.put("httpMethod", aggregate.method());
        apiInformationDraft.put("method", aggregate.method());
        apiInformationDraft.put("url", aggregate.servicePathTemplate());
        apiInformationDraft.put("statusCode", aggregate.statusCodes().stream().findFirst().orElse(null));
        apiInformationDraft.put("plIn", aggregate.requestPayloadTemplate());
        apiInformationDraft.put("mediaType", aggregate.contentTypes().stream().findFirst().orElse("application/json"));
        return apiInformationDraft;
    }

    private boolean hasMatchedApiInformation(Map<String, Object> logicalStep) {
        return logicalStep != null && logicalStep.get("matchedApiInformation") instanceof Map<?, ?>;
    }

    private List<Map<String, Object>> buildEndpoints(Collection<EndpointAggregate> endpointAggregates) {
        List<Map<String, Object>> endpoints = new ArrayList<>();
        int endpointOrder = 1;
        for (EndpointAggregate aggregate : endpointAggregates) {
            LinkedHashMap<String, Object> endpoint = new LinkedHashMap<>();
            endpoint.put("endpointOrder", endpointOrder++);
            endpoint.put("method", aggregate.method());
            endpoint.put("baseUrl", aggregate.baseUrl());
            endpoint.put("path", aggregate.path());
            endpoint.put("normalizedPath", aggregate.normalizedPath());
            endpoint.put("occurrenceCount", aggregate.occurrenceCount());
            endpoint.put("statusCodes", new ArrayList<>(aggregate.statusCodes()));
            endpoint.put("contentTypes", new ArrayList<>(aggregate.contentTypes()));
            endpoint.put("queryParameterNames", new ArrayList<>(aggregate.queryParameterNames()));
            endpoint.put("queryParameters", new LinkedHashMap<>(aggregate.queryParameters()));
            endpoint.put("requestSample", aggregate.requestSample());
            endpoint.put("responseSample", aggregate.responseSample());
            endpoint.put("averageResponseTimeMs", aggregate.averageDurationMs());
            endpoint.put("maxResponseTimeMs", aggregate.maxDurationMs());
            endpoint.put("sourceEntryIndexes", new ArrayList<>(aggregate.sourceEntryIndexes()));
            endpoint.put("matchesRequestedSystem", aggregate.matchesRequestedSystem());
            endpoints.add(endpoint);
        }
        return endpoints;
    }

    private List<Map<String, Object>> buildLogicalSteps(Collection<EndpointAggregate> endpointAggregates, List<ApiInformationDto> projectApis) {
        List<Map<String, Object>> logicalSteps = new ArrayList<>();
        int stepOrder = 1;
        for (EndpointAggregate aggregate : endpointAggregates) {
            LinkedHashMap<String, Object> logicalStep = new LinkedHashMap<>();
            String stepShortCodeSuggestion = buildStepShortCode(aggregate.method(), aggregate.path(), stepOrder);
            List<ApiMatchCandidate> apiMatchCandidates = buildApiMatchCandidates(aggregate, projectApis);
            Map<String, Object> matchedApiInformation = apiMatchCandidates.stream()
                    .filter(ApiMatchCandidate::autoMatch)
                    .findFirst()
                    .map(this::toApiMatch)
                    .orElse(null);
            List<Map<String, Object>> candidateApiInformation = apiMatchCandidates.stream()
                    .limit(5)
                    .map(this::toApiMatch)
                    .collect(Collectors.toCollection(ArrayList::new));
            logicalStep.put("stepOrder", stepOrder);
            logicalStep.put("stepShortCode", stepShortCodeSuggestion);
            logicalStep.put("stepShortCodeSuggestion", stepShortCodeSuggestion);
            logicalStep.put("stepType", "REQUEST_GROUP");
            logicalStep.put("title", aggregate.method() + " " + aggregate.normalizedPath());
            logicalStep.put("summary", buildLogicalStepSummary(aggregate));
            logicalStep.put("method", aggregate.method());
            logicalStep.put("baseUrl", aggregate.baseUrl());
            logicalStep.put("url", aggregate.baseUrl() + aggregate.path());
            logicalStep.put("path", aggregate.path());
            logicalStep.put("normalizedPath", aggregate.normalizedPath());
            logicalStep.put("servicePathTemplate", aggregate.servicePathTemplate());
            logicalStep.put("occurrenceCount", aggregate.occurrenceCount());
            logicalStep.put("expectedStatusCodes", new ArrayList<>(aggregate.statusCodes()));
            logicalStep.put("statusCode", aggregate.statusCodes().stream().findFirst().orElse(null));
            logicalStep.put("responseTime", aggregate.averageDurationMs());
            logicalStep.put("sourceRequestOrders", new ArrayList<>(aggregate.sourceEntryIndexes()));
            logicalStep.put("queryParameterNames", new ArrayList<>(aggregate.queryParameterNames()));
            logicalStep.put("queryParameters", new LinkedHashMap<>(aggregate.queryParameters()));
            logicalStep.put("plIn", aggregate.requestPayloadTemplate());
            logicalStep.put("requestPayloadTemplate", aggregate.requestPayloadTemplate());
            logicalStep.put("processFlowStepParmList", aggregate.processFlowStepParameters());
            logicalStep.put("requestSample", aggregate.requestSample());
            logicalStep.put("responseSample", aggregate.responseSample());
            logicalStep.put("sourceEntryIndexes", new ArrayList<>(aggregate.sourceEntryIndexes()));
            logicalStep.put("matchedApiInformation", matchedApiInformation);
            logicalStep.put("candidateApiInformation", candidateApiInformation);
            logicalStep.put("apiInformationDraft", matchedApiInformation == null ? buildApiInformationDraft(aggregate, stepShortCodeSuggestion) : null);
            logicalStep.put(
                    "matchedApiShortCode",
                    matchedApiInformation == null
                            ? null
                            : StringUtils.defaultIfBlank(
                                    Objects.toString(matchedApiInformation.get("apiShortCode"), null),
                                    Objects.toString(matchedApiInformation.get("shortCode"), null)
                            )
            );
            logicalStep.put("included", true);
            logicalSteps.add(logicalStep);
            stepOrder++;
        }
        return logicalSteps;
    }

    private String buildLogicalStepSummary(EndpointAggregate aggregate) {
        List<Integer> statusCodes = new ArrayList<>(aggregate.statusCodes());
        String statusSummary = statusCodes.isEmpty()
                ? "Durum kodu gözlemlenmedi"
                : statusCodes.stream().map(String::valueOf).collect(Collectors.joining(", "));
        return aggregate.occurrenceCount()
                + " istek gözlemlendi; durum kodları: "
                + statusSummary
                + "; ortalama yanıt süresi: "
                + aggregate.averageDurationMs()
                + " ms.";
    }

    private List<Map<String, Object>> buildRelationships(
            List<ProcessedRequest> processedRequests,
            List<Map<String, Object>> logicalSteps,
            Map<EndpointKey, Integer> stepOrderByEndpoint
    ) {
        Map<Integer, Map<String, Object>> stepByOrder = logicalSteps.stream()
                .collect(Collectors.toMap(
                        step -> Integer.parseInt(String.valueOf(step.get("stepOrder"))),
                        step -> step,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, Integer> transitionCounts = new LinkedHashMap<>();

        for (int index = 1; index < processedRequests.size(); index++) {
            ProcessedRequest previousRequest = processedRequests.get(index - 1);
            ProcessedRequest currentRequest = processedRequests.get(index);
            Integer fromStepOrder = stepOrderByEndpoint.get(previousRequest.endpointKey());
            Integer toStepOrder = stepOrderByEndpoint.get(currentRequest.endpointKey());
            if (fromStepOrder == null || toStepOrder == null || Objects.equals(fromStepOrder, toStepOrder)) {
                continue;
            }
            String transitionKey = fromStepOrder + "->" + toStepOrder;
            transitionCounts.merge(transitionKey, 1, Integer::sum);
        }

        List<Map<String, Object>> relationships = new ArrayList<>();
        for (Map.Entry<String, Integer> transitionEntry : transitionCounts.entrySet()) {
            String[] stepOrders = transitionEntry.getKey().split("->");
            Integer fromStepOrder = Integer.parseInt(stepOrders[0]);
            Integer toStepOrder = Integer.parseInt(stepOrders[1]);
            Map<String, Object> fromStep = stepByOrder.get(fromStepOrder);
            Map<String, Object> toStep = stepByOrder.get(toStepOrder);
            if (fromStep == null || toStep == null) {
                continue;
            }

            LinkedHashMap<String, Object> relationship = new LinkedHashMap<>();
            relationship.put("type", "SEQUENCE");
            relationship.put("sourceStepOrder", fromStepOrder);
            relationship.put("targetStepOrder", toStepOrder);
            relationship.put("sourceType", StringUtils.defaultIfBlank(Objects.toString(fromStep.get("method"), null), Objects.toString(fromStep.get("stepType"), null)));
            relationship.put("sourcePath", StringUtils.defaultIfBlank(Objects.toString(fromStep.get("path"), null), Objects.toString(fromStep.get("normalizedPath"), null)));
            relationship.put("targetType", StringUtils.defaultIfBlank(Objects.toString(toStep.get("method"), null), Objects.toString(toStep.get("stepType"), null)));
            relationship.put("targetPath", StringUtils.defaultIfBlank(Objects.toString(toStep.get("path"), null), Objects.toString(toStep.get("normalizedPath"), null)));
            relationship.put("fromStepOrder", fromStepOrder);
            relationship.put("fromStepShortCodeSuggestion", fromStep.get("stepShortCodeSuggestion"));
            relationship.put("toStepOrder", toStepOrder);
            relationship.put("toStepShortCodeSuggestion", toStep.get("stepShortCodeSuggestion"));
            relationship.put("transitionCount", transitionEntry.getValue());
            relationship.put("confidence", 100);
            relationship.put("reason", transitionEntry.getValue() + " kez HAR kaydında ardışık olarak gözlemlendi.");
            relationships.add(relationship);
        }
        return relationships;
    }

    private String buildFlowShortCodeSuggestion(
            String projectShortCode,
            String systemShortCode,
            List<Map<String, Object>> logicalSteps,
            Collection<EndpointAggregate> endpointAggregates
    ) {
        String primaryFlowKeyword = logicalSteps.stream()
                .map(step -> Objects.toString(step.get("stepShortCodeSuggestion"), ""))
                .map(code -> StringUtils.substringAfter(code, "_"))
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElseGet(() -> endpointAggregates.stream()
                        .map(EndpointAggregate::path)
                        .map(this::deriveKeywordFromPath)
                        .filter(StringUtils::isNotBlank)
                        .findFirst()
                        .orElse("HAR_REVIEW"));

        return sanitizeShortCode(projectShortCode + "_" + systemShortCode + "_" + primaryFlowKeyword + "_FLOW");
    }

    private String deriveKeywordFromPath(String path) {
        return Arrays.stream(StringUtils.defaultIfBlank(path, "/").split("/"))
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .filter(segment -> !GENERIC_FLOW_SEGMENTS.contains(StringUtils.lowerCase(segment, Locale.ROOT)))
                .map(this::normalizeSegmentForShortCode)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .orElse("HAR_REVIEW");
    }

    private String buildSummary(
            List<ProcessedRequest> processedRequests,
            Collection<EndpointAggregate> endpointAggregates,
            Set<String> discoveredBaseUrls,
            String flowShortCodeSuggestion
    ) {
        return processedRequests.size()
                + " istek, "
                + endpointAggregates.size()
                + " benzersiz endpoint ve "
                + discoveredBaseUrls.size()
                + " base URL üzerinden analiz edildi. Önerilen akış kısa kodu: "
                + flowShortCodeSuggestion
                + ".";
    }

    private Map<String, Object> buildStatistics(
            int totalEntries,
            List<ProcessedRequest> processedRequests,
            List<Map<String, Object>> ignoredRequests,
            Collection<EndpointAggregate> endpointAggregates,
            Set<String> discoveredBaseUrls,
            Set<String> requestedSystemBaseUrls
    ) {
        LinkedHashMap<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("totalEntries", totalEntries);
        statistics.put("analyzedRequestCount", processedRequests.size());
        statistics.put("ignoredRequestCount", ignoredRequests.size());
        statistics.put("distinctEndpointCount", endpointAggregates.size());
        statistics.put("distinctBaseUrlCount", discoveredBaseUrls.size());
        statistics.put("requestedSystemBaseUrlCount", requestedSystemBaseUrls.size());

        LinkedHashMap<String, Long> methodCounts = processedRequests.stream()
                .collect(Collectors.groupingBy(ProcessedRequest::method, LinkedHashMap::new, Collectors.counting()));
        statistics.put("methodCounts", methodCounts);

        LinkedHashMap<String, Long> statusCodeCounts = processedRequests.stream()
                .collect(Collectors.groupingBy(request -> String.valueOf(request.status()), LinkedHashMap::new, Collectors.counting()));
        statistics.put("statusCodeCounts", statusCodeCounts);

        long totalDuration = processedRequests.stream().mapToLong(ProcessedRequest::durationMs).sum();
        statistics.put("averageResponseTimeMs", processedRequests.isEmpty() ? 0L : Math.round((double) totalDuration / processedRequests.size()));
        statistics.put("failingRequestCount", processedRequests.stream().filter(request -> request.status() >= 400).count());
        statistics.put("matchingRequestedSystemEndpointCount", endpointAggregates.stream().filter(EndpointAggregate::matchesRequestedSystem).count());
        return statistics;
    }

    private List<String> buildWarnings(
            List<ProcessedRequest> processedRequests,
            List<Map<String, Object>> ignoredRequests,
            Set<String> discoveredBaseUrls,
            Collection<EndpointAggregate> endpointAggregates,
            Set<String> requestedSystemBaseUrls,
            Set<String> availableSystemCodes,
            String systemShortCode
    ) {
        LinkedHashSet<String> warnings = new LinkedHashSet<>();

        if (processedRequests.isEmpty()) {
            warnings.add("Analiz edilebilecek API isteği bulunamadı.");
        }
        if (discoveredBaseUrls.size() > 1) {
            warnings.add("Birden fazla base URL tespit edildi; review aşamasında doğru sistem kapsamını daraltın.");
        }
        if (!requestedSystemBaseUrls.isEmpty() && endpointAggregates.stream().anyMatch(aggregate -> !aggregate.matchesRequestedSystem())) {
            warnings.add("Bazı endpoint'ler seçilen systemShortCode ile eşleşen base URL dışında görünüyor.");
        }
        if (endpointAggregates.stream().anyMatch(aggregate -> aggregate.statusCodes().stream().anyMatch(status -> status >= 400))) {
            warnings.add("4xx veya 5xx durum kodu dönen istekler mevcut; akış taslağına almadan önce doğrulayın.");
        }
        if (!availableSystemCodes.isEmpty() && !availableSystemCodes.contains(systemShortCode)) {
            warnings.add("Verilen systemShortCode proje üzerindeki bilinen sistem kodları arasında bulunamadı: " + String.join(", ", availableSystemCodes));
        }
        if (ignoredRequests.size() > processedRequests.size() && !ignoredRequests.isEmpty()) {
            warnings.add("Analiz dışı bırakılan istek sayısı analiz edilen isteklerden fazla; filtreleri review sırasında gözden geçirin.");
        }

        return new ArrayList<>(warnings);
    }

    private List<String> buildRecommendations(
            List<ProcessedRequest> processedRequests,
            Collection<EndpointAggregate> endpointAggregates,
            List<Map<String, Object>> ignoredRequests,
            Set<String> discoveredBaseUrls,
            List<String> warnings
    ) {
        LinkedHashSet<String> recommendations = new LinkedHashSet<>();
        recommendations.add("Review aşamasında processFlowDraft içindeki step'leri kontrol edip gereksiz olanları dışarı alın.");
        recommendations.add("Logical step'leri mevcut API tanımları ile eşleştirmeden önce path ve status code tutarlılığını doğrulayın.");

        if (endpointAggregates.stream().anyMatch(aggregate -> aggregate.occurrenceCount() > 1)) {
            recommendations.add("Tekrarlayan endpoint'lerin retry mi yoksa bilinçli tekrar mı olduğunu işaretleyin.");
        }
        if (discoveredBaseUrls.size() > 1) {
            recommendations.add("Birden fazla base URL bulunduğu için review sırasında doğru systemShortCode kapsamını daraltın.");
        }
        if (!ignoredRequests.isEmpty()) {
            recommendations.add("Ignored requests listesini kontrol ederek gerçekten hariç kalması gereken kayıtları doğrulayın.");
        }
        if (!warnings.isEmpty() && processedRequests.stream().anyMatch(request -> request.status() >= 400)) {
            recommendations.add("Başarısız istekler için blocker veya unresolved step kaydı oluşturun.");
        }

        return new ArrayList<>(recommendations);
    }

    private Map<String, Object> buildProcessFlowDraft(
            ProjectDto project,
            String projectShortCode,
            String systemShortCode,
            String flowShortCodeSuggestion,
            String summary,
            List<Map<String, Object>> logicalSteps,
            List<Map<String, Object>> relationships
    ) {
        LinkedHashMap<String, Object> processFlowDraft = new LinkedHashMap<>();
        Map<Integer, List<Integer>> dependenciesByStep = new LinkedHashMap<>();
        for (Map<String, Object> relationship : relationships) {
            Integer sourceStepOrder = relationship.get("sourceStepOrder") instanceof Number sourceNumber
                    ? sourceNumber.intValue()
                    : relationship.get("fromStepOrder") instanceof Number fromNumber
                    ? fromNumber.intValue()
                    : null;
            Integer targetStepOrder = relationship.get("targetStepOrder") instanceof Number targetNumber
                    ? targetNumber.intValue()
                    : relationship.get("toStepOrder") instanceof Number toNumber
                    ? toNumber.intValue()
                    : null;
            if (sourceStepOrder != null && targetStepOrder != null) {
                dependenciesByStep.computeIfAbsent(targetStepOrder, key -> new ArrayList<>()).add(sourceStepOrder);
            }
        }

        processFlowDraft.put("projectId", project.getProjectId());
        processFlowDraft.put("projectShortCode", projectShortCode);
        processFlowDraft.put("projectName", project.getName());
        processFlowDraft.put("systemShortCode", systemShortCode);
        processFlowDraft.put("shortCode", flowShortCodeSuggestion);
        processFlowDraft.put("flowShortCode", flowShortCodeSuggestion);
        processFlowDraft.put("name", project.getName() + " " + flowShortCodeSuggestion);
        processFlowDraft.put("isActive", true);
        processFlowDraft.put("summary", summary);
        processFlowDraft.put("stepCount", logicalSteps.size());
        processFlowDraft.put("relationships", relationships);

        List<Map<String, Object>> draftSteps = new ArrayList<>();
        for (Map<String, Object> logicalStep : logicalSteps) {
            Integer stepOrder = logicalStep.get("stepOrder") instanceof Number number ? number.intValue() : null;
            @SuppressWarnings("unchecked")
            Map<String, Object> matchedApiInformation = logicalStep.get("matchedApiInformation") instanceof Map<?, ?>
                    ? (Map<String, Object>) logicalStep.get("matchedApiInformation")
                    : null;
            LinkedHashMap<String, Object> draftStep = new LinkedHashMap<>();
            draftStep.put("stepOrder", logicalStep.get("stepOrder"));
            draftStep.put("stepShortCode", logicalStep.get("stepShortCodeSuggestion"));
            draftStep.put("stepType", logicalStep.get("stepType"));
            draftStep.put("title", logicalStep.get("title"));
            draftStep.put("summary", logicalStep.get("summary"));
            draftStep.put("method", logicalStep.get("method"));
            draftStep.put("baseUrl", logicalStep.get("baseUrl"));
            draftStep.put("path", logicalStep.get("path"));
            draftStep.put("normalizedPath", logicalStep.get("normalizedPath"));
            draftStep.put("servicePathTemplate", logicalStep.get("servicePathTemplate"));
            draftStep.put("expectedStatusCodes", logicalStep.get("expectedStatusCodes"));
            draftStep.put("dependsOnStepOrders", stepOrder == null ? List.of() : new ArrayList<>(dependenciesByStep.getOrDefault(stepOrder, List.of())));
            draftStep.put("sourceRequestOrders", logicalStep.get("sourceRequestOrders"));
            draftStep.put("plIn", logicalStep.get("plIn"));
            draftStep.put("requestPayloadTemplate", logicalStep.get("requestPayloadTemplate"));
            draftStep.put("processFlowStepParmList", logicalStep.get("processFlowStepParmList"));
            draftStep.put("requestSample", logicalStep.get("requestSample"));
            draftStep.put("responseSample", logicalStep.get("responseSample"));
            draftStep.put("gnlApiInformationId", matchedApiInformation == null ? null : matchedApiInformation.get("id"));
            draftStep.put("matchedApiShortCode", matchedApiInformation == null
                    ? null
                    : StringUtils.defaultIfBlank(
                            Objects.toString(matchedApiInformation.get("apiShortCode"), null),
                            Objects.toString(matchedApiInformation.get("shortCode"), null)
                    ));
            draftStep.put("apiInformationDraft", logicalStep.get("apiInformationDraft"));
            draftStep.put("included", true);
            draftSteps.add(draftStep);
        }
        processFlowDraft.put("steps", draftSteps);
        return processFlowDraft;
    }

    private String buildStepShortCode(String method, String path, int stepOrder) {
        List<String> segmentParts = Arrays.stream(StringUtils.defaultIfBlank(path, "/").split("/"))
                .map(StringUtils::trimToNull)
                .filter(Objects::nonNull)
                .filter(segment -> !GENERIC_FLOW_SEGMENTS.contains(StringUtils.lowerCase(segment, Locale.ROOT)))
                .map(this::normalizeSegmentForShortCode)
                .filter(StringUtils::isNotBlank)
                .limit(3)
                .toList();

        String suffix = segmentParts.isEmpty()
                ? String.format(Locale.ROOT, "STEP_%02d", stepOrder)
                : String.join("_", segmentParts);
        return sanitizeShortCode(method + "_" + suffix);
    }

    private String normalizeSegmentForShortCode(String segment) {
        String normalizedSegment = StringUtils.defaultString(segment).replace("{id}", "BY_ID").replace("{token}", "BY_TOKEN");
        normalizedSegment = normalizedSegment.replaceAll("[^A-Za-z0-9]+", "_");
        normalizedSegment = normalizedSegment.replaceAll("_+", "_");
        normalizedSegment = StringUtils.strip(normalizedSegment, "_");
        if (StringUtils.isBlank(normalizedSegment)) {
            return null;
        }
        return StringUtils.upperCase(normalizedSegment, Locale.ROOT);
    }

    private String sanitizeShortCode(String shortCode) {
        String normalizedShortCode = StringUtils.defaultString(shortCode).replaceAll("[^A-Za-z0-9_]+", "_");
        normalizedShortCode = normalizedShortCode.replaceAll("_+", "_");
        normalizedShortCode = StringUtils.strip(normalizedShortCode, "_");
        return StringUtils.left(StringUtils.upperCase(normalizedShortCode, Locale.ROOT), 80);
    }

    private Map<String, Object> buildIgnoredRequest(int sourceEntryIndex, String method, String requestUrl, String path, String reason) {
        LinkedHashMap<String, Object> ignoredRequest = new LinkedHashMap<>();
        ignoredRequest.put("sourceEntryIndex", sourceEntryIndex);
        ignoredRequest.put("method", method);
        ignoredRequest.put("url", requestUrl);
        if (StringUtils.isNotBlank(path)) {
            ignoredRequest.put("path", path);
        }
        ignoredRequest.put("reason", reason);
        return ignoredRequest;
    }

    private record ApiMatchCandidate(
            ApiInformationDto apiInformation,
            int score,
            boolean autoMatch,
            List<String> reasons
    ) {
    }

    private record RequestCapture(
            Map<String, String> queryParameters,
            Map<String, String> requestHeaders,
            String requestBodySample,
            boolean requestBodyTruncated,
            Long requestSizeBytes,
            String requestPayloadTemplate,
            String servicePathTemplate,
            List<Map<String, Object>> processFlowStepParameters
    ) {
    }

    private record ResponseCapture(
            Map<String, String> responseHeaders,
            String responseBodySample,
            boolean responseBodyTruncated,
            Long responseSizeBytes
    ) {
    }

    private record EndpointKey(String method, String baseUrl, String normalizedPath) {
    }

    private record ProcessedRequest(
            int sourceEntryIndex,
            EndpointKey endpointKey,
            String method,
            String baseUrl,
            String path,
            String normalizedPath,
            int status,
            long durationMs
    ) {
    }

    private static final class EndpointAggregate {

        private final String method;
        private final String baseUrl;
        private final String path;
        private final String normalizedPath;
        private final int firstEntryIndex;
        private int occurrenceCount;
        private long totalDurationMs;
        private long maxDurationMs;
        private boolean matchesRequestedSystem;
        private final LinkedHashSet<Integer> statusCodes = new LinkedHashSet<>();
        private final LinkedHashSet<String> contentTypes = new LinkedHashSet<>();
        private final LinkedHashSet<String> queryParameterNames = new LinkedHashSet<>();
        private final LinkedHashSet<Integer> sourceEntryIndexes = new LinkedHashSet<>();
        private final LinkedHashMap<String, String> queryParameters = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> requestHeaders = new LinkedHashMap<>();
        private final LinkedHashMap<String, String> responseHeaders = new LinkedHashMap<>();
        private final List<Map<String, Object>> processFlowStepParameters = new ArrayList<>();
        private String requestBodySample;
        private boolean requestBodyTruncated;
        private Long requestSizeBytes;
        private String requestPayloadTemplate;
        private String servicePathTemplate;
        private String responseBodySample;
        private boolean responseBodyTruncated;
        private Long responseSizeBytes;

        private EndpointAggregate(String method, String baseUrl, String path, String normalizedPath, int firstEntryIndex) {
            this.method = method;
            this.baseUrl = baseUrl;
            this.path = path;
            this.normalizedPath = normalizedPath;
            this.firstEntryIndex = firstEntryIndex;
        }

        private void recordOccurrence(int status, String mimeType, long durationMs, List<String> queryParameterNames, int sourceEntryIndex, boolean matchesRequestedSystem, RequestCapture requestCapture, ResponseCapture responseCapture) {
            occurrenceCount++;
            totalDurationMs += durationMs;
            maxDurationMs = Math.max(maxDurationMs, durationMs);
            this.matchesRequestedSystem = this.matchesRequestedSystem || matchesRequestedSystem;
            if (status > 0) {
                statusCodes.add(status);
            }
            if (StringUtils.isNotBlank(mimeType)) {
                contentTypes.add(mimeType);
            }
            if (queryParameterNames != null) {
                this.queryParameterNames.addAll(queryParameterNames);
            }
            sourceEntryIndexes.add(sourceEntryIndex);
            if (occurrenceCount == 1 && requestCapture != null) {
                queryParameters.putAll(requestCapture.queryParameters());
                requestHeaders.putAll(requestCapture.requestHeaders());
                requestBodySample = requestCapture.requestBodySample();
                requestBodyTruncated = requestCapture.requestBodyTruncated();
                requestSizeBytes = requestCapture.requestSizeBytes();
                requestPayloadTemplate = requestCapture.requestPayloadTemplate();
                servicePathTemplate = requestCapture.servicePathTemplate();
                processFlowStepParameters.addAll(requestCapture.processFlowStepParameters());
            }
            if (occurrenceCount == 1 && responseCapture != null) {
                responseHeaders.putAll(responseCapture.responseHeaders());
                responseBodySample = responseCapture.responseBodySample();
                responseBodyTruncated = responseCapture.responseBodyTruncated();
                responseSizeBytes = responseCapture.responseSizeBytes();
            }
        }

        private String method() {
            return method;
        }

        private String baseUrl() {
            return baseUrl;
        }

        private String path() {
            return path;
        }

        private String normalizedPath() {
            return normalizedPath;
        }

        private int firstEntryIndex() {
            return firstEntryIndex;
        }

        private int occurrenceCount() {
            return occurrenceCount;
        }

        private long averageDurationMs() {
            return occurrenceCount == 0 ? 0L : Math.round((double) totalDurationMs / occurrenceCount);
        }

        private long maxDurationMs() {
            return maxDurationMs;
        }

        private boolean matchesRequestedSystem() {
            return matchesRequestedSystem;
        }

        private LinkedHashSet<Integer> statusCodes() {
            return statusCodes;
        }

        private LinkedHashSet<String> contentTypes() {
            return contentTypes;
        }

        private LinkedHashSet<String> queryParameterNames() {
            return queryParameterNames;
        }

        private LinkedHashSet<Integer> sourceEntryIndexes() {
            return sourceEntryIndexes;
        }

        private LinkedHashMap<String, String> queryParameters() {
            return queryParameters;
        }

        private String requestPayloadTemplate() {
            return StringUtils.defaultString(requestPayloadTemplate);
        }

        private String servicePathTemplate() {
            return StringUtils.defaultIfBlank(servicePathTemplate, normalizedPath);
        }

        private List<Map<String, Object>> processFlowStepParameters() {
            return processFlowStepParameters.stream()
                    .map(parameter -> new LinkedHashMap<String, Object>(parameter))
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private Map<String, Object> requestSample() {
            LinkedHashMap<String, Object> requestSample = new LinkedHashMap<>();
            requestSample.put("headers", new LinkedHashMap<>(requestHeaders));
            requestSample.put("queryParameters", new LinkedHashMap<>(queryParameters));
            requestSample.put("body", requestBodySample);
            requestSample.put("bodyTruncated", requestBodyTruncated);
            requestSample.put("sizeBytes", requestSizeBytes);
            return requestSample;
        }

        private Map<String, Object> responseSample() {
            LinkedHashMap<String, Object> responseSample = new LinkedHashMap<>();
            responseSample.put("headers", new LinkedHashMap<>(responseHeaders));
            responseSample.put("body", responseBodySample);
            responseSample.put("bodyTruncated", responseBodyTruncated);
            responseSample.put("sizeBytes", responseSizeBytes);
            return responseSample;
        }
    }
}
