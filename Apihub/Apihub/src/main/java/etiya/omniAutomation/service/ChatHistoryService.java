package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.ChatMessageDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatHistoryService {

    @Value("${chat.history.expiration-minutes:3}")
    private int expirationMinutes;

    @Value("${chat.system.message:Sen bir performans testi asistanısın. Kullanıcılara performans testleri, API çağrıları ve test otomasyonu konularında yardımcı oluyorsun. Türkçe yanıt ver.}")
    private String systemMessage;

    private final Map<String, List<ChatMessageDto>> userChatHistories = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastActivityTimes = new ConcurrentHashMap<>();

    public List<ChatMessageDto> getChatHistory(String userId) {
        updateLastActivity(userId);
        return userChatHistories.computeIfAbsent(userId, k -> {
            List<ChatMessageDto> history = new ArrayList<>();
            history.add(new ChatMessageDto("system", systemMessage, null, null, null, null));
            return history;
        });
    }

    public void addMessage(String userId, ChatMessageDto message) {
        List<ChatMessageDto> history = getChatHistory(userId);
        history.add(message);
        updateLastActivity(userId);
    }

    public void clearHistory(String userId) {
        userChatHistories.remove(userId);
        lastActivityTimes.remove(userId);
    }

    public void clearAllHistories() {
        userChatHistories.clear();
        lastActivityTimes.clear();
    }

    private void updateLastActivity(String userId) {
        lastActivityTimes.put(userId, LocalDateTime.now());
    }

    @Scheduled(fixedRate = 60000) // Her dakika çalışır
    public void cleanupExpiredHistories() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredUsers = new ArrayList<>();

        lastActivityTimes.forEach((userId, lastActivity) -> {
            if (lastActivity.plusMinutes(expirationMinutes).isBefore(now)) {
                expiredUsers.add(userId);
            }
        });

        expiredUsers.forEach(this::clearHistory);
    }
}
