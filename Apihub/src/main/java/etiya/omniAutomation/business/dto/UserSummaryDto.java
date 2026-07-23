package etiya.omniAutomation.business.dto;

import java.io.Serializable;

public record UserSummaryDto(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String authType,
        int enabled
) implements Serializable { }
