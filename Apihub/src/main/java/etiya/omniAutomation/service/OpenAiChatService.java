package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.ChatRequest;
import etiya.omniAutomation.business.dto.ChatMessageDto;
import etiya.omniAutomation.business.dto.ChatStreamEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OpenAiChatService {

    private static final String HAR_CONTEXT_SYSTEM_PROMPT = """
            Bu yardımcı yalnızca kurumsal iş bağlamında ve mevcut tenant/proje kapsamı içinde yanıt verir.
            Çok kiracılı bir yapı olduğu için mevcut istek bağlamında açıkça verilmeyen başka proje, müşteri, tenant, sistem veya ortamlar hakkında bilgi verme, tahmin yürütme veya karşılaştırma yapma.
            Sadece mevcut istekte verilen proje bağlamını, sistem bağlamını ve yapılandırılmış çalışma verisini kaynak gerçek olarak kullan.
            Kullanıcının sorusu işle ilgisizse veya kurumsal çalışma kapsamı dışındaysa bunu kısa şekilde belirt ve yalnızca işle ilgili konularda yardımcı olabileceğini söyle.
            Kullanıcı mevcut proje kapsamı dışında başka bir proje, tenant veya müşteriye ait bilgi isterse bunu reddet ve yalnızca aktif proje bağlamında yardımcı olabileceğini belirt.
            Soru proje bağlamı gerektiriyor ancak mevcut istekte yeterli proje bilgisi yoksa cevap üretmeden önce projectShortCode ve gerekiyorsa systemShortCode bilgisini iste.
            Bu konuşmada sağlanan yapılandırılmış bağlam, HAR analiz katmanı tarafından üretilmiş ve gerektiğinde kullanıcı tarafından gözden geçirilmiş çalışma verisidir.
            HAR dosyasını yeniden ayrıştırma veya iş kurallarını yeniden üretme.
            Sağlanan bağlamı kaynak gerçek olarak kullan.
            Eksik, çözümlenmemiş veya riskli alanları açıkça belirt.
            Eğer en güncel review durumunu tekrar kontrol etmen gerekiyorsa get_current_har_review_context tool'unu çağır.
            Gerçek sistem verisi, mevcut API tanımları, akış bilgileri veya aksiyonlar için mevcut MCP tool'larını kullan.
            Kullanıcı mesajlarını RAG TOOL kullanarak ilk önce araştır. Eğer mantıklı bir sonuç dönmüyorsa bu soru proje ile ilgili değildir.
            Eğer kullanıcının mesajı boşsa, mevcut HAR review durumunu özetle, blokajları çıkar ve önerilen bir sonraki adımı söyle.
            """;
    private static final String RESPONSE_FORMAT_SYSTEM_PROMPT = """
            Yardımcı yanıtını her zaman tam olarak iki XML blok halinde üret:
            <thinking>...</thinking>
            <answer>...</answer>
            
            Kurallar:
            - <thinking> içinde kullanıcıya gösterilebilecek kısa ve yüksek seviyeli bir yaklaşım özeti ver.
            - İçsel veya gizli chain-of-thought, ayrıntılı iç muhakeme, ham ara çıkarımlar veya güvenlik/politika içeriği yazma.
            - <thinking> bölümünü kısa tut: en fazla 3 madde veya kısa bir paragraf.
            - Nihai kullanıcı cevabını sadece <answer> içinde ver.
            - XML etiketlerinin dışına hiçbir metin yazma.
            - Markdown gerekiyorsa sadece <answer> içinde kullan.
            """;
    private static final String THINKING_OPEN_TAG = "<thinking>";
    private static final String THINKING_CLOSE_TAG = "</thinking>";
    private static final String ANSWER_OPEN_TAG = "<answer>";
    private static final String ANSWER_CLOSE_TAG = "</answer>";
    private static final int STRUCTURED_OUTPUT_FALLBACK_THRESHOLD = 48;

    private final ChatClient.Builder chatClientBuilder;
    private final ToolCallbackProvider toolCallbackProvider;
    private final ChatContextBuilderService chatContextBuilderService;

    public Flux<ChatStreamEventDto> streamMessage(List<ChatMessageDto> conversationHistory, ChatRequest request) {
        Prompt prompt = new Prompt(toSpringAiMessages(conversationHistory, request));
        ToolCallback[] toolCallbacks = toolCallbackProvider.getToolCallbacks();
        StructuredResponseStreamState streamState = new StructuredResponseStreamState();

        return chatClientBuilder.build()
                .prompt(prompt)
                .toolCallbacks(toolCallbacks)
                .stream()
                .chatResponse()
                .filter(Objects::nonNull)
                .concatMap(chatResponse -> Flux.fromIterable(toStreamEvents(chatResponse, streamState)))
                .concatWith(Flux.fromIterable(streamState.flush()))
                .concatWithValues(ChatStreamEventDto.done())
                .onErrorResume(error -> Flux.just(ChatStreamEventDto.error(error.getMessage())));
    }

    private List<Message> toSpringAiMessages(List<ChatMessageDto> conversationHistory) {
        return toSpringAiMessages(conversationHistory, null);
    }

    private List<Message> toSpringAiMessages(List<ChatMessageDto> conversationHistory, ChatRequest request) {
        List<Message> messages = new ArrayList<>();
        for (ChatMessageDto message : conversationHistory) {
            messages.add(toSpringAiMessage(message));
        }
        injectStructuredContext(messages, request);
        injectResponseFormatInstruction(messages);
        return messages;
    }

    private void injectStructuredContext(List<Message> messages, ChatRequest request) {
        if (request == null) {
            return;
        }

        String chatContext = chatContextBuilderService.buildChatContext(request);
        boolean hasProjectScope = request.getProjectShortCode() != null && !request.getProjectShortCode().isBlank();
        boolean hasSystemScope = request.getSystemShortCode() != null && !request.getSystemShortCode().isBlank();

        if ((chatContext == null || chatContext.isBlank()) && !hasProjectScope && !hasSystemScope) {
            return;
        }

        int insertIndex = getLeadingSystemMessageCount(messages);
        messages.add(insertIndex, new SystemMessage(buildHarContextSystemPrompt(request, chatContext)));
    }

    private void injectResponseFormatInstruction(List<Message> messages) {
        int insertIndex = getLeadingSystemMessageCount(messages);
        messages.add(insertIndex, new SystemMessage(RESPONSE_FORMAT_SYSTEM_PROMPT));
    }

    private String buildHarContextSystemPrompt(ChatRequest request, String chatContext) {
        StringBuilder promptBuilder = new StringBuilder(HAR_CONTEXT_SYSTEM_PROMPT);

        if (request != null) {
            String projectShortCode = request.getProjectShortCode();
            String systemShortCode = request.getSystemShortCode();

            if ((projectShortCode != null && !projectShortCode.isBlank()) || (systemShortCode != null && !systemShortCode.isBlank())) {
                promptBuilder.append("\n\nAktif istek kapsamı:");

                if (projectShortCode != null && !projectShortCode.isBlank()) {
                    promptBuilder.append("\n- projectShortCode: ").append(projectShortCode);
                }

                if (systemShortCode != null && !systemShortCode.isBlank()) {
                    promptBuilder.append("\n- systemShortCode: ").append(systemShortCode);
                }

                promptBuilder.append("\nBu kapsamın dışına çıkma ve başka proje/tenant bilgisi üretme.");
            }
        }

        if (chatContext != null && !chatContext.isBlank()) {
            promptBuilder.append("\n\nYapılandırılmış bağlam:\n").append(chatContext);
        }

        return promptBuilder.toString();
    }

    private int getLeadingSystemMessageCount(List<Message> messages) {
        int insertIndex = 0;
        while (insertIndex < messages.size() && messages.get(insertIndex) instanceof SystemMessage) {
            insertIndex++;
        }
        return insertIndex;
    }

    private Message toSpringAiMessage(ChatMessageDto message) {
        return switch (message.getRole()) {
            case "system" -> new SystemMessage(message.getContent());
            case "assistant" -> new AssistantMessage(message.getContent());
            default -> new UserMessage(message.getContent());
        };
    }

    private List<ChatStreamEventDto> toStreamEvents(ChatResponse chatResponse, StructuredResponseStreamState streamState) {
        List<ChatStreamEventDto> events = new ArrayList<>();

        ChatResponseMetadata metadata = chatResponse.getMetadata();
        Generation result = chatResponse.getResult();
        if (result != null && result.getOutput() != null && result.getOutput().getText() != null && !result.getOutput().getText().isEmpty()) {
            events.addAll(streamState.consumeContentChunk(result.getOutput().getText()));
            String reasoningContent = extractReasoning(result.getOutput().getMetadata());
            events.addAll(streamState.consumeMetadataReasoning(reasoningContent));
        }

        if (metadata == null) {
            return events;
        }

        Usage usage = metadata.getUsage();
        if (usage != null) {
            events.add(ChatStreamEventDto.usage(
                    usage.getPromptTokens(),
                    usage.getCompletionTokens(),
                    usage.getTotalTokens()
            ));

            String reasoning = extractReasoning(usage.getNativeUsage());
            events.addAll(streamState.consumeMetadataReasoning(reasoning));
        }

        return events;
    }

    private String extractReasoning(Object nativeUsage) {
        if (!(nativeUsage instanceof Map<?, ?> nativeUsageMap)) {
            return null;
        }

        Object reasoning = nativeUsageMap.get("reasoningContent");
        if (reasoning == null) {
            reasoning = nativeUsageMap.get("reasoning");
        }
        if (reasoning == null) {
            reasoning = nativeUsageMap.get("reasoning_content");
        }
        if (reasoning == null) {
            reasoning = nativeUsageMap.get("thoughts");
        }
        return reasoning != null ? String.valueOf(reasoning) : null;
    }

    private static String extractSection(String rawText, String openTag, String closeTag, String nextOpenTag) {
        int openIndex = rawText.indexOf(openTag);
        if (openIndex < 0) {
            return "";
        }

        int sectionStart = openIndex + openTag.length();
        int closeIndex = rawText.indexOf(closeTag, sectionStart);
        if (closeIndex >= 0) {
            return rawText.substring(sectionStart, closeIndex);
        }

        if (nextOpenTag != null) {
            int nextOpenIndex = rawText.indexOf(nextOpenTag, sectionStart);
            if (nextOpenIndex >= 0) {
                return rawText.substring(sectionStart, nextOpenIndex);
            }
        }

        return stripTrailingIncompleteTag(rawText.substring(sectionStart), closeTag, nextOpenTag);
    }

    private static String stripTrailingIncompleteTag(String value, String... tags) {
        String cleanedValue = value;
        for (String tag : tags) {
            if (tag == null || tag.isEmpty()) {
                continue;
            }
            int cutIndex = findTrailingPartialTagStart(cleanedValue, tag);
            if (cutIndex < cleanedValue.length()) {
                cleanedValue = cleanedValue.substring(0, cutIndex);
            }
        }
        return cleanedValue;
    }

    private static int findTrailingPartialTagStart(String text, String tag) {
        int maxPrefixLength = Math.min(text.length(), tag.length() - 1);
        for (int prefixLength = maxPrefixLength; prefixLength > 0; prefixLength--) {
            if (text.regionMatches(text.length() - prefixLength, tag, 0, prefixLength)) {
                return text.length() - prefixLength;
            }
        }
        return text.length();
    }

    private static int firstPositiveIndex(int... indexes) {
        int smallestIndex = Integer.MAX_VALUE;
        for (int index : indexes) {
            if (index >= 0 && index < smallestIndex) {
                smallestIndex = index;
            }
        }
        return smallestIndex == Integer.MAX_VALUE ? -1 : smallestIndex;
    }

    private record ParsedAssistantOutput(String reasoning, String answer) {
    }

    private final class StructuredResponseStreamState {

        private final StringBuilder rawText = new StringBuilder();
        private int emittedReasoningLength;
        private int emittedAnswerLength;
        private int emittedMetadataReasoningLength;
        private boolean plainTextFallback;

        private List<ChatStreamEventDto> consumeContentChunk(String chunk) {
            if (chunk == null || chunk.isEmpty()) {
                return List.of();
            }

            rawText.append(chunk);
            return buildDeltaEvents(parseCurrent());
        }

        private List<ChatStreamEventDto> consumeMetadataReasoning(String reasoning) {
            if (reasoning == null || reasoning.isBlank() || rawText.indexOf(THINKING_OPEN_TAG) >= 0 || emittedReasoningLength > 0) {
                return List.of();
            }

            if (reasoning.length() <= emittedMetadataReasoningLength) {
                return List.of();
            }

            String delta = reasoning.substring(emittedMetadataReasoningLength);
            emittedMetadataReasoningLength = reasoning.length();
            emittedReasoningLength = Math.max(emittedReasoningLength, emittedMetadataReasoningLength);
            return List.of(ChatStreamEventDto.reasoning(delta));
        }

        private List<ChatStreamEventDto> flush() {
            if (!plainTextFallback && !hasStructuredMarkers(rawText.toString()) && rawText.length() > 0) {
                plainTextFallback = true;
            }
            return buildDeltaEvents(parseCurrent());
        }

        private ParsedAssistantOutput parseCurrent() {
            String currentRawText = rawText.toString();
            if (currentRawText.isBlank()) {
                return new ParsedAssistantOutput("", "");
            }

            if (!plainTextFallback && !hasStructuredMarkers(currentRawText) && currentRawText.stripLeading().length() >= STRUCTURED_OUTPUT_FALLBACK_THRESHOLD) {
                plainTextFallback = true;
            }

            if (plainTextFallback) {
                return new ParsedAssistantOutput("", currentRawText);
            }

            int firstTagIndex = firstPositiveIndex(currentRawText.indexOf(THINKING_OPEN_TAG), currentRawText.indexOf(ANSWER_OPEN_TAG));
            String leadingText = firstTagIndex > 0 ? currentRawText.substring(0, firstTagIndex) : "";
            String reasoning = extractSection(currentRawText, THINKING_OPEN_TAG, THINKING_CLOSE_TAG, ANSWER_OPEN_TAG);
            String answer = extractSection(currentRawText, ANSWER_OPEN_TAG, ANSWER_CLOSE_TAG, null);

            if (!leadingText.isBlank()) {
                answer = leadingText + answer;
            }

            return new ParsedAssistantOutput(reasoning, answer);
        }

        private List<ChatStreamEventDto> buildDeltaEvents(ParsedAssistantOutput parsedOutput) {
            List<ChatStreamEventDto> events = new ArrayList<>();

            if (parsedOutput.reasoning().length() > emittedReasoningLength) {
                events.add(ChatStreamEventDto.reasoning(parsedOutput.reasoning().substring(emittedReasoningLength)));
                emittedReasoningLength = parsedOutput.reasoning().length();
            }

            if (parsedOutput.answer().length() > emittedAnswerLength) {
                events.add(ChatStreamEventDto.content(parsedOutput.answer().substring(emittedAnswerLength)));
                emittedAnswerLength = parsedOutput.answer().length();
            }

            return events;
        }

        private boolean hasStructuredMarkers(String value) {
            return value.contains(THINKING_OPEN_TAG) || value.contains(ANSWER_OPEN_TAG);
        }
    }
}
