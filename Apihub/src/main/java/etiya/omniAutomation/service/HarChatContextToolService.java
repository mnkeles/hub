package etiya.omniAutomation.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HarChatContextToolService {

    private final ChatStructuredContextSessionService chatStructuredContextSessionService;

    public HarChatContextToolService(ChatStructuredContextSessionService chatStructuredContextSessionService) {
        this.chatStructuredContextSessionService = chatStructuredContextSessionService;
    }

    @Tool(name = "get_current_har_review_context", description = "Aktif kullanicinin UI uzerinden chat akisina gonderdigi mevcut HAR analiz ve review baglamini getirir. Cozulmemis step'leri, secili API eslesmelerini, warnings alanlarini veya son draft durumunu kontrol etmek icin kullan.")
    public Map<String, Object> getCurrentHarReviewContext() {
        String userId = resolveCurrentUserId();
        Map<String, Object> context = new LinkedHashMap<>(chatStructuredContextSessionService.getCurrentStructuredContext(userId));
        context.put("userId", userId);
        return context;
    }

    private String resolveCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
}
