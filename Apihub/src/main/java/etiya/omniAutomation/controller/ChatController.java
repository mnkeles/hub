package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.ChatMessageDto;
import etiya.omniAutomation.business.dto.ChatRequest;
import etiya.omniAutomation.business.dto.ChatStreamEventDto;
import etiya.omniAutomation.service.ChatHistoryService;
import etiya.omniAutomation.service.ChatStructuredContextSessionService;
import etiya.omniAutomation.service.OpenAiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ChatController {

    private final OpenAiChatService openAiChatService;
    private final ChatHistoryService chatHistoryService;
    private final ChatStructuredContextSessionService chatStructuredContextSessionService;

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatStreamEventDto> streamMessage(@RequestBody ChatRequest request) {
        String userId = getCurrentUserId();
        ChatRequest effectiveRequest = chatStructuredContextSessionService.mergeWithStoredContext(userId, request);
        String messageContent = StringUtils.defaultIfBlank(effectiveRequest.getMessage(), "Mevcut bağlama göre yardımcı ol.");

        ChatMessageDto userMessage = new ChatMessageDto("user", messageContent, null, null, null, null);
        chatHistoryService.addMessage(userId, userMessage);

        List<ChatMessageDto> history = chatHistoryService.getChatHistory(userId);
        AtomicReference<StringBuilder> fullResponse = new AtomicReference<>(new StringBuilder());
        AtomicReference<StringBuilder> reasoningResponse = new AtomicReference<>(new StringBuilder());
        AtomicReference<Integer> promptTokens = new AtomicReference<>(null);
        AtomicReference<Integer> completionTokens = new AtomicReference<>(null);
        AtomicReference<Integer> totalTokens = new AtomicReference<>(null);

        return openAiChatService.streamMessage(history, effectiveRequest)
                .doOnNext(event -> {
                    if ("content".equals(event.getType()) && event.getContent() != null) {
                        fullResponse.get().append(event.getContent());
                    } else if ("usage".equals(event.getType())) {
                        promptTokens.set(event.getPromptTokens());
                        completionTokens.set(event.getCompletionTokens());
                        totalTokens.set(event.getTotalTokens());
                    } else if ("reasoning".equals(event.getType()) && event.getReasoning() != null) {
                        reasoningResponse.get().append(event.getReasoning());
                    }
                })
                .doOnComplete(() -> {
                    String completeResponse = fullResponse.get().toString();
                    String completeReasoning = reasoningResponse.get().toString();
                    if (!completeResponse.isEmpty() || !StringUtils.isBlank(completeReasoning)) {
                        ChatMessageDto assistantMessage = new ChatMessageDto(
                                "assistant",
                                completeResponse,
                                StringUtils.isBlank(completeReasoning) ? null : completeReasoning,
                                promptTokens.get(),
                                completionTokens.get(),
                                totalTokens.get()
                        );
                        chatHistoryService.addMessage(userId, assistantMessage);
                    }
                })
                .doOnError(error -> log.error("Chat stream hatası - User: {}", userId, error));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageDto>> getChatHistory() {
        try {
            String userId = getCurrentUserId();
            return ResponseEntity.ok(chatHistoryService.getChatHistory(userId));
        }
        catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/context/current")
    public ResponseEntity<Map<String, Object>> getCurrentStructuredContext() {
        try {
            String userId = getCurrentUserId();
            return ResponseEntity.ok(chatStructuredContextSessionService.getCurrentStructuredContext(userId));
        }
        catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/history")
    public ResponseEntity<Void> clearChatHistory() {
        try {
            String userId = getCurrentUserId();
            chatHistoryService.clearHistory(userId);
            chatStructuredContextSessionService.clearContext(userId);
            return ResponseEntity.ok().build();
        }
        catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chat service is running");
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}
