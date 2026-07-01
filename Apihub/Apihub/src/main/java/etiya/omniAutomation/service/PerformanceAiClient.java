package etiya.omniAutomation.service;

public interface PerformanceAiClient {
    String complete(String systemPrompt, String userPrompt);

    String modelName();
}
