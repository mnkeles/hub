package etiya.omniAutomation.business.dto;

import java.util.List;

public record CurrentUserResponse(
        String username,
        String authType,
        String firstName,
        String lastName,
        int enabled,
        List<String> permissions
) { }
