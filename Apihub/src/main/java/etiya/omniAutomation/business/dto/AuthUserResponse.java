package etiya.omniAutomation.business.dto;

import java.util.List;

public record AuthUserResponse(
        String id,
        String username,
        String email,
        String authType,
        List<String> permissions
) { }
