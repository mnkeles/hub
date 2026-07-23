package etiya.omniAutomation.business.dto;

import java.time.Instant;

public record ServiceTokenCreateResponse(
        Long serviceTokenId,
        String tokenName,
        String token,
        String tokenPrefix,
        Instant expiresAt
) { }
