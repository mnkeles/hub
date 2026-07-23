package etiya.omniAutomation.business.dto;

import java.time.Instant;

public record ServiceTokenCreateRequest(
        String tokenName,
        Instant expiresAt
) { }
