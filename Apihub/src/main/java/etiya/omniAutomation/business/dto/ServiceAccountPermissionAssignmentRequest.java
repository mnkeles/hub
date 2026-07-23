package etiya.omniAutomation.business.dto;

import java.util.List;

public record ServiceAccountPermissionAssignmentRequest(
        List<PermissionProjectAssignment> assignments
) {
    public record PermissionProjectAssignment(Long permissionId, Long projectId) { }
}
