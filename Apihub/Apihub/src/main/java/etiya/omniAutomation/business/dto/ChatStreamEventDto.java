package etiya.omniAutomation.business.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatStreamEventDto {
    private String type;
    private String content;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private String reasoning;
    private String error;

    public static ChatStreamEventDto content(String content) {
        return new ChatStreamEventDto("content", content, null, null, null, null, null);
    }

    public static ChatStreamEventDto usage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        return new ChatStreamEventDto("usage", null, promptTokens, completionTokens, totalTokens, null, null);
    }

    public static ChatStreamEventDto reasoning(String reasoning) {
        return new ChatStreamEventDto("reasoning", null, null, null, null, reasoning, null);
    }

    public static ChatStreamEventDto done() {
        return new ChatStreamEventDto("done", null, null, null, null, null, null);
    }

    public static ChatStreamEventDto error(String error) {
        return new ChatStreamEventDto("error", null, null, null, null, null, error);
    }
}
