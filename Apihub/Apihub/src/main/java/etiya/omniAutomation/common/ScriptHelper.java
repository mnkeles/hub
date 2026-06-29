package etiya.omniAutomation.common;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Map;

public class ScriptHelper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String process(Map<String, String> parameterContext, String responseBody, String script) {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("graal.js");
        if (scriptEngine == null) {
            throw new IllegalStateException("JavaScript engine bulunamadı.");
        }
        try {
            parameterContext.forEach((key, value) -> {
                try {
                    if (value != null && isJsonString(value)) {
                        try {
                            scriptEngine.eval("var tempJson = " + value + ";");
                            Object parsedValue = scriptEngine.get("tempJson");
                            scriptEngine.put(key, parsedValue);
                        } catch (Exception e) {
                            try {
                                Object parsedValue = objectMapper.readValue(value, Object.class);
                                scriptEngine.put(key, parsedValue);
                            } catch (Exception e2) {
                                scriptEngine.put(key, value);
                            }
                        }
                    } else {
                        scriptEngine.put(key, value);
                    }
                } catch (Exception e) {
                    scriptEngine.put(key, value);
                }
            });

            scriptEngine.put("response", responseBody);
            //scriptEngine.put("parameterContext", parameterContext);
            String jsonParseScript =
                    "var responseBody = null; " +
                            "try { " +
                            "  responseBody = JSON.parse(response); " +
                            "} catch(e) { " +
                            "  console.log('JSON parse error: ' + e.message); " +
                            "  responseBody = null; " +
                            "}" +
                            "if (typeof quote === 'string') {" +
                            "  try {" +
                            "    quote = JSON.parse(quote);" +
                            "    console.log('Successfully parsed quote string to object');" +
                            "  } catch(e) {" +
                            "    console.log('Failed to parse quote string: ' + e.message);" +
                            "  }" +
                            "}";

            scriptEngine.eval(jsonParseScript);
            Object jsonResult = scriptEngine.eval("JSON.stringify(" + script + ")");

            return jsonResult != null ? jsonResult.toString() : null;
        } catch (Exception e) {
            throw new RuntimeException("Script çalıştırma hatası: " + e.getMessage(), e);
        }
    }

    private boolean isJsonString(String str) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        String trimmed = str.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    public String process( String script) {
        ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("graal.js");
        if (scriptEngine == null) {
            throw new IllegalStateException("JavaScript engine bulunamadı.");
        }
        try {
            String jsonParseScript =
                    "var responseBody = null; " +
                            "try { " +
                            "  responseBody = JSON.parse(response); " +
                            "} catch(e) { " +
                            "  console.log('JSON parse error: ' + e.message); " +
                            "  responseBody = null; " +
                            "}" +
                            "if (typeof quote === 'string') {" +
                            "  try {" +
                            "    quote = JSON.parse(quote);" +
                            "    console.log('Successfully parsed quote string to object');" +
                            "  } catch(e) {" +
                            "    console.log('Failed to parse quote string: ' + e.message);" +
                            "  }" +
                            "}";

            scriptEngine.eval(jsonParseScript);
            Object jsonResult = scriptEngine.eval("JSON.stringify(" + script + ")");

            return jsonResult != null ? jsonResult.toString() : null;
        } catch (Exception e) {
            throw new RuntimeException("Script çalıştırma hatası: " + e.getMessage(), e);
        }
    }
}