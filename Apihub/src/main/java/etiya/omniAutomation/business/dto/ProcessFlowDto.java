package etiya.omniAutomation.business.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.Gson;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import etiya.omniAutomation.common.ScriptHelper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import org.w3c.dom.Document;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class ProcessFlowDto extends AbstractDto {

    private Long processFlowId;
    private String shortCode;
    private String isActive;
    private String systemShortCode;
    private String systemShortCodeOAB;
    private Long projectId;
    private List<ProcessFlowStepDto> processFlowStepList;
    private List<ProcessFlowStepRelationDto> processFlowStepRelations;
    
    @JsonIgnore
    private Map<String, String> globalHeaders;
    
    @JsonIgnore
    private LinkedHashMap<String, String> parameterContext;
    
    @JsonIgnore
    private transient Gson gson;
    
    @JsonIgnore
    private transient ExpressionParser expressionParser;
    
    @JsonIgnore
    private transient ScriptEngineManager scriptEngineManager;
    
    // Lazy initialization için helper metodları
    private Gson getOrCreateGson() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }
    
    private ExpressionParser getOrCreateExpressionParser() {
        if (expressionParser == null) {
            expressionParser = new SpelExpressionParser();
        }
        return expressionParser;
    }
    
    private ScriptEngineManager getOrCreateScriptEngineManager() {
        if (scriptEngineManager == null) {
            scriptEngineManager = new ScriptEngineManager();
        }
        return scriptEngineManager;
    }


    public void processHeaders(ResponseEntity<String> response, String headerExtractor) {
        if (StringUtils.isNotBlank(headerExtractor)) {
            HttpHeaders responseHeaders = response.getHeaders();
            Arrays.stream(StringUtils.split(headerExtractor, ';'))
                    .forEach(item -> {
                        if (StringUtils.equalsIgnoreCase(item, "Authorization")) {
                            this.globalHeaders.put("LocalToken", "1");
                            // Aynı şartlar altında position-id ekliyoruz
//                            this.globalHeaders.put("position-id", "10000717680573");
                        }
                        this.globalHeaders.put(item, responseHeaders.getFirst(item));
                    });
        }
    }

    public void processParameters(ResponseEntity<String> response, String parameterExtractor) {
        if (StringUtils.isNotBlank(parameterExtractor)) {
            String body = StringUtils.trim(response.getBody());
            processWithoutSpel(parameterExtractor, body);
        }
        //OMNI akışlarında kullanılması için responsedan dönen Location headers bilgisinin bir kısmı parameterContext içine eklendi, static bir geliştirme oldu
        if (Objects.nonNull(response.getHeaders().get("Location"))) {
            String location = Objects.requireNonNull(response.getHeaders().get("Location")).getFirst();

            URI locationUri = URI.create(location);
            String[] segments = locationUri.getPath().split("/");
            String id = segments[segments.length - 1];

            this.parameterContext.put("Location", id);
        }
    }

    private String processScript(String parameterExtractor, String response) {
        Expression expression = getOrCreateExpressionParser().parseExpression(parameterExtractor);
        EvaluationContext context = new StandardEvaluationContext(); // root object boş
        context.setVariable("response", response);
        context.setVariable("engine", new ScriptHelper());
        context.setVariable("parameterContext", this.parameterContext); // map null değil
        return expression.getValue(context, String.class);
    }

    private void processWithoutSpel(String parameterExtractor, String body) {
        Arrays.stream(StringUtils.split(parameterExtractor, "|||"))
                .forEach(item -> {
                    int index = item.indexOf('&');
                    if (index > 0 && index < item.length() - 1) {
                        String key = item.substring(0, index);
                        String path = item.substring(index + 1);
                        processPath(body, path, key);
                    }
                });
    }

    private void processPath(String body, String path, String key) {
        String value = null;
        if (path.charAt(0) == '#') {
            value = processScript(path.substring(1), body);
        } else {
            try {
                value = extractValueAsString(body, path);
            } catch (Exception e) {
                System.err.println("Path parse hatası: " + path + " -> " + e.getMessage());
            }
        }
        if (value != null) {
            this.parameterContext.put(key, value);
        }
    }


    private String extractValueAsString(String body, String path) throws Exception {
        // JSONPath gibi görünüyor mu?
        if (path.startsWith("$")) {
            Object value = JsonPath.read(body, path);
            return (value instanceof String || value instanceof Number || value instanceof Boolean) ?
                    value.toString() : getOrCreateGson().toJson(value);
        }
        else if (path.startsWith("/")) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false); // namespace yokmuş gibi davran
            factory.setIgnoringElementContentWhitespace(true);

            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

            XPath xPath = XPathFactory.newInstance().newXPath();
            String value = (String) xPath.evaluate(path, doc, XPathConstants.STRING);

            return StringUtils.isBlank(value) ? null : value.trim();
        }

        throw new IllegalArgumentException("Desteklenmeyen path formatı: " + path);
    }

    public String executeRegex(String regex, String source) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher((source));
        if (matcher.find()) {
            return matcher.group(1);
        }
        log.warn("regex null");
        return null;
    }

    public String executeJsonPath(String path, String source) {
        return JsonPath.read(org.springframework.util.StringUtils.trimAllWhitespace(source), path);
    }
    
    // Override Lombok generated getters with @JsonIgnore
    @JsonIgnore
    public Map<String, String> getGlobalHeaders() {
        if (globalHeaders == null) {
            globalHeaders = new HashMap<>();
        }
        return globalHeaders;
    }
    
    @JsonIgnore
    public LinkedHashMap<String, String> getParameterContext() {
        if (parameterContext == null) {
            parameterContext = new LinkedHashMap<>();
        }
        return parameterContext;
    }
    
    @JsonIgnore
    public Gson getGson() {
        if (gson == null) {
            gson = new Gson();
        }
        return gson;
    }
    
    @JsonIgnore
    public ExpressionParser getExpressionParser() {
        if (expressionParser == null) {
            expressionParser = new SpelExpressionParser();
        }
        return expressionParser;
    }
    
    @JsonIgnore
    public ScriptEngineManager getScriptEngineManager() {
        if (scriptEngineManager == null) {
            scriptEngineManager = new ScriptEngineManager();
        }
        return scriptEngineManager;
    }
}
