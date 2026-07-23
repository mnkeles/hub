package etiya.omniAutomation.business.dto;

import java.util.List;

public record UserPermissionAssignmentRequest(
        List<Long> permissionIds
) { }
