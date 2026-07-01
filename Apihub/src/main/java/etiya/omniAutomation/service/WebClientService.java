package etiya.omniAutomation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WebClientService {

    private final WebClient webClient;

    public <T> ResponseEntity<T> exchange(String url, HttpEntity<?> httpEntity, HttpHeaders headers, HttpMethod httpMethod, ParameterizedTypeReference<T> typeReference) {
        WebClient.RequestBodySpec bodySpec = webClient.method(httpMethod)
                .uri(URI.create(url))
                .headers(httpHeaders -> httpHeaders.setAll(headers.toSingleValueMap()));
        if ((Objects.nonNull(httpEntity) && httpEntity.hasBody())) {
            if (headers.getContentType() != null && headers.getContentType().includes(MediaType.APPLICATION_FORM_URLENCODED)) {
                if (httpEntity.getBody() instanceof String) {
                    MultiValueMap<String, String> formData = parseFormBody((String) httpEntity.getBody());
                    bodySpec = (WebClient.RequestBodySpec) bodySpec.body(BodyInserters.fromFormData(formData));
                }
            } else {
                bodySpec = (WebClient.RequestBodySpec) bodySpec.bodyValue(httpEntity.getBody());
            }
        }
        return bodySpec.retrieve()
                .toEntity(typeReference)
                .block();
    }

    private MultiValueMap<String, String> parseFormBody(String body) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        String[] pairs = body.split("&");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                formData.add(key, value);
            }
        }

        return formData;
    }

}
