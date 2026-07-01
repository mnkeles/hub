package etiya.omniAutomation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import etiya.omniAutomation.business.dto.HarAnalysisResultDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HarAnalysisSessionService {

    @Value("${har.analysis.expiration-minutes:${chat.context.expiration-minutes:${chat.history.expiration-minutes:15}}}")
    private int expirationMinutes;

    private final ObjectMapper objectMapper;
    private final Map<String, StoredHarAnalysis> storedAnalyses = new ConcurrentHashMap<>();

    public HarAnalysisSessionService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String storeAnalysis(String userId, HarAnalysisResultDto analysisResult) {
        String analysisReferenceId = UUID.randomUUID().toString();
        LinkedHashMap<String, Object> payload = objectMapper.convertValue(
                analysisResult,
                new TypeReference<LinkedHashMap<String, Object>>() {
                }
        );
        payload.put("analysisReferenceId", analysisReferenceId);
        storedAnalyses.put(analysisReferenceId, new StoredHarAnalysis(userId, payload));
        return analysisReferenceId;
    }

    public Object getAnalysisPayload(String userId, String analysisReferenceId) {
        if (StringUtils.isBlank(analysisReferenceId)) {
            return null;
        }

        StoredHarAnalysis storedHarAnalysis = storedAnalyses.get(analysisReferenceId);
        if (storedHarAnalysis == null || !StringUtils.equals(userId, storedHarAnalysis.userId())) {
            return null;
        }

        storedHarAnalysis.touch();
        return objectMapper.convertValue(storedHarAnalysis.payload(), Object.class);
    }

    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredAnalyses() {
        LocalDateTime now = LocalDateTime.now();
        storedAnalyses.entrySet().removeIf(entry -> entry.getValue().lastAccessedAt().plusMinutes(expirationMinutes).isBefore(now));
    }

    private static final class StoredHarAnalysis {

        private final String userId;
        private final Map<String, Object> payload;
        private volatile LocalDateTime lastAccessedAt;

        private StoredHarAnalysis(String userId, Map<String, Object> payload) {
            this.userId = userId;
            this.payload = payload;
            this.lastAccessedAt = LocalDateTime.now();
        }

        private String userId() {
            return userId;
        }

        private Map<String, Object> payload() {
            return payload;
        }

        private LocalDateTime lastAccessedAt() {
            return lastAccessedAt;
        }

        private void touch() {
            lastAccessedAt = LocalDateTime.now();
        }
    }
}
