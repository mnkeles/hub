package etiya.omniAutomation.business.dto;

public record CurrentUserResponse(
        String username,
        String authType,
        String firstName,
        String lastName,
        int enabled,
        Long projectId
) { }
