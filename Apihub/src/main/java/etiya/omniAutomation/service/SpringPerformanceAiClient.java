package etiya.omniAutomation.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class SpringPerformanceAiClient implements PerformanceAiClient {

    private final ChatClient chatClient;

    public SpringPerformanceAiClient(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        return chatClient
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
