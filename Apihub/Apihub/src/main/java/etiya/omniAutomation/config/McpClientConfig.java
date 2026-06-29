package etiya.omniAutomation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
 import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class McpClientConfig {

    @Value("${mcp.server.url:${spring.ai.mcp.client.sse.connections.apihub.url:http://localhost:9999}}")
    private String mcpServerUrl;

    @Value("${mcp.client.max-in-memory-size-bytes:52428800}")
    private int maxInMemorySizeBytes;

    @Bean
    public WebClient mcpWebClient() {
        log.info("Creating MCP WebClient for URL: {} with maxInMemorySizeBytes: {}", mcpServerUrl, maxInMemorySizeBytes);
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySizeBytes))
                .build();

        return WebClient.builder()
                .baseUrl(mcpServerUrl)
                .exchangeStrategies(exchangeStrategies)
                .filter(propagateAuthorizationHeader())
                .build();
    }

    private ExchangeFilterFunction propagateAuthorizationHeader() {
        return (request, next) -> {
            String authorizationHeader = resolveCurrentAuthorizationHeader();
            if (authorizationHeader == null || authorizationHeader.isBlank() || request.headers().containsKey(HttpHeaders.AUTHORIZATION)) {
                return next.exchange(request);
            }

            ClientRequest authenticatedRequest = ClientRequest.from(request)
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader))
                    .build();
            return next.exchange(authenticatedRequest);
        };
    }

    private String resolveCurrentAuthorizationHeader() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletRequestAttributes)) {
            return null;
        }
        return servletRequestAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
    }
}
