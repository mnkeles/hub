package etiya.omniAutomation.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

public final class CommonUtilities {

    public static String jsonMinify(String body){
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readValue(body, JsonNode.class);
            return jsonNode.toString();
        }catch (Exception e){
            return e.getMessage();
        }
    }

    public static String getToken(String url) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("userName", "Etiya_Admin");
            jsonObject.put("password", "aa1234");
            String json = CommonUtilities.jsonMinify(jsonObject.toString());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(json, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url.concat("/auth"), entity, String.class);
            return response.getHeaders().getFirst("Authorization");
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static HttpHeaders addHttpHeaders(HttpHeaders headers, String headerParameters) {
        String[] headersSplit = headerParameters.split(";");
        for (int i = 0; i < Arrays.stream(headersSplit).count(); i++) {
            String[] headerSplit = headersSplit[i].split(":");
            String headerName = headerSplit[0];
            String headerValue = headerSplit[1];
            if (headerName.contains("Collation") && headerValue.contains("fn")) {
                continue;
            } else {
                headers.set(headerName, headerValue);
            }
        }
        return headers;
    }

    }
