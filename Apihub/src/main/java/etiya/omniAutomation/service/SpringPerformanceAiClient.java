package etiya.omniAutomation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpringPerformanceAiClient implements PerformanceAiClient {

    private final ChatClient.Builder chatClientBuilder;

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return chatClientBuilder.build()
                .prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    @Override
    public String modelName() {
        return "spring-ai-openai";
    }
}
