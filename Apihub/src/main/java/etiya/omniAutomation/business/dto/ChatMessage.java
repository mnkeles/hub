package etiya.omniAutomation.business.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private String role; // "user", "assistant", "system"
    private String content;
    
    public static class Role {
        public static final String USER = "user";
        public static final String ASSISTANT = "assistant";
        public static final String SYSTEM = "system";
    }
}
