package etiya.omniAutomation.business.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDto {
    private String role; // "user", "assistant", "system"
    private String content;
    private String reasoning;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}
