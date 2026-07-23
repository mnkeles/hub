package etiya.omniAutomation.business.dto;

import java.util.List;

public record UserPermissionsResponseDto(
        Long userId,
        String email,
        List<PermissionDto> permissions
) { }
