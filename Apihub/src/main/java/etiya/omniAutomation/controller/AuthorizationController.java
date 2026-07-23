package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.PermissionDto;
import etiya.omniAutomation.business.dto.ServiceAccountDto;
import etiya.omniAutomation.business.dto.ServiceAccountPermissionAssignmentRequest;
import etiya.omniAutomation.business.dto.ServiceAccountRequest;
import etiya.omniAutomation.business.dto.ServiceTokenCreateRequest;
import etiya.omniAutomation.business.dto.ServiceTokenCreateResponse;
import etiya.omniAutomation.business.dto.UserPermissionAssignmentRequest;
import etiya.omniAutomation.business.dto.UserPermissionsResponseDto;
import etiya.omniAutomation.business.dto.UserSummaryDto;
import etiya.omniAutomation.common.PermissionConstants;
import etiya.omniAutomation.service.AuthorizationAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/authorization")
@RequiredArgsConstructor
public class AuthorizationController {

    private final AuthorizationAdminService authorizationAdminService;

    @GetMapping("/permissions")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERMISSION_MANAGE + "')")
    public ResponseEntity<List<PermissionDto>> getPermissions() {
        return ResponseEntity.ok(authorizationAdminService.getPermissions());
    }

    @GetMapping("/users")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERMISSION_MANAGE + "')")
    public ResponseEntity<List<UserSummaryDto>> getUsers() {
        return ResponseEntity.ok(authorizationAdminService.getUsers());
    }

    @GetMapping("/users/{userId}/permissions")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERMISSION_MANAGE + "')")
    public ResponseEntity<UserPermissionsResponseDto> getUserPermissions(@PathVariable Long userId) {
        return ResponseEntity.ok(authorizationAdminService.getUserPermissions(userId));
    }

    @PutMapping("/users/{userId}/permissions")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.PERMISSION_MANAGE + "')")
    public ResponseEntity<Void> assignUserPermissions(@PathVariable Long userId, @RequestBody UserPermissionAssignmentRequest request) {
        authorizationAdminService.assignUserPermissions(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/service-accounts")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.SERVICE_ACCOUNT_MANAGE + "')")
    public ResponseEntity<List<ServiceAccountDto>> getServiceAccounts() {
        return ResponseEntity.ok(authorizationAdminService.getServiceAccounts());
    }

    @PostMapping("/service-accounts")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.SERVICE_ACCOUNT_MANAGE + "')")
    public ResponseEntity<ServiceAccountDto> createServiceAccount(@RequestBody ServiceAccountRequest request) {
        return ResponseEntity.ok(authorizationAdminService.createServiceAccount(request));
    }

    @PutMapping("/service-accounts/{serviceAccountId}/permissions")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.SERVICE_ACCOUNT_MANAGE + "')")
    public ResponseEntity<Void> assignServiceAccountPermissions(@PathVariable Long serviceAccountId, @RequestBody ServiceAccountPermissionAssignmentRequest request) {
        authorizationAdminService.assignServiceAccountPermissions(serviceAccountId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/service-accounts/{serviceAccountId}/tokens")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.SERVICE_ACCOUNT_MANAGE + "')")
    public ResponseEntity<ServiceTokenCreateResponse> createServiceToken(@PathVariable Long serviceAccountId, @RequestBody ServiceTokenCreateRequest request, Authentication authentication) {
        return ResponseEntity.ok(authorizationAdminService.createServiceToken(serviceAccountId, request, authentication));
    }

    @PostMapping("/service-tokens/{tokenId}/revoke")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.SERVICE_ACCOUNT_MANAGE + "')")
    public ResponseEntity<Void> revokeServiceToken(@PathVariable Long tokenId) {
        authorizationAdminService.revokeServiceToken(tokenId);
        return ResponseEntity.ok().build();
    }
}
