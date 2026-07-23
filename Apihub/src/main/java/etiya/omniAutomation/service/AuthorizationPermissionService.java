package etiya.omniAutomation.service;

import etiya.omniAutomation.common.PermissionConstants;
import etiya.omniAutomation.config.security.AuthorizationUserDetails;
import etiya.omniAutomation.entity.ServiceTokenEntity;
import etiya.omniAutomation.entity.UserEntity;
import etiya.omniAutomation.repository.ServiceAccountPermissionRepository;
import etiya.omniAutomation.repository.ServiceTokenRepository;
import etiya.omniAutomation.repository.UserProjectRelationRepository;
import etiya.omniAutomation.repository.UserRepository;
import etiya.omniAutomation.repository.UserPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service("authorizationPermissionService")
@RequiredArgsConstructor
public class AuthorizationPermissionService {

    private static final String LOCAL_USER = "LOCAL_USER";
    private static final String LDAP_USER = "LDAP_USER";
    private static final String SERVICE_ACCOUNT = "SERVICE_ACCOUNT";
    private static final String SERVICE_TOKEN_PREFIX = "apihub_svc_";

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final ServiceAccountPermissionRepository serviceAccountPermissionRepository;
    private final ServiceTokenRepository serviceTokenRepository;
    private final UserProjectRelationRepository userProjectRelationRepository;

    @Cacheable(value = "authUserDetails", key = "#email")
    public AuthorizationUserDetails loadUserDetails(String email) {
        UserEntity user = userRepository.findByEmailAndEnabled(email, 1)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        List<String> permissions = userPermissionRepository.findPermissionKeysByUserEmail(email);
        boolean superUser = permissions.contains(PermissionConstants.SUPER_USER);
        List<Long> projectIds = userProjectRelationRepository.findProjectIdsByEmail(user.getUserId());
        String principalType = "LDAP".equals(user.getAuthType()) ? LDAP_USER : LOCAL_USER;
        return new AuthorizationUserDetails(
                user.getEmail(),
                user.getPassword() != null ? user.getPassword() : "",
                principalType,
                user.getUserId(),
                superUser ? List.of("*") : distinct(permissions),
                distinct(projectIds),
                superUser,
                false,
                user.getEnabled() == 1
        );
    }

    @Transactional
    public Optional<AuthorizationUserDetails> authenticateServiceToken(String token) {
        if (token == null || !token.startsWith(SERVICE_TOKEN_PREFIX)) {
            return Optional.empty();
        }

        ServiceTokenEntity serviceToken = serviceTokenRepository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new BadCredentialsException("Invalid service token"));

        if (serviceToken.getRevokedAt() != null) {
            throw new BadCredentialsException("Service token is revoked");
        }
        if (serviceToken.getExpiresAt() != null && serviceToken.getExpiresAt().toInstant().isBefore(Instant.now())) {
            throw new BadCredentialsException("Service token is expired");
        }
        if (serviceToken.getServiceAccountEntity() == null || serviceToken.getServiceAccountEntity().getEnabled() != 1) {
            throw new BadCredentialsException("Service account is disabled");
        }

        Long serviceAccountId = serviceToken.getServiceAccountEntity().getServiceAccountId();
        List<String> permissions = serviceAccountPermissionRepository.findPermissionKeysByServiceAccountId(serviceAccountId);
        boolean superUser = permissions.contains(PermissionConstants.SUPER_USER);
        List<Long> projectIds = serviceAccountPermissionRepository.findProjectIdsByServiceAccountId(serviceAccountId);
        boolean globalProjectAccess = serviceAccountPermissionRepository.hasGlobalAccess(serviceAccountId);
        serviceToken.setLastUsedAt(new Date());
        serviceTokenRepository.save(serviceToken);

        AuthorizationUserDetails details = new AuthorizationUserDetails(
                serviceToken.getServiceAccountEntity().getServiceCode(),
                "",
                SERVICE_ACCOUNT,
                serviceAccountId,
                superUser ? List.of("*") : distinct(permissions),
                distinct(projectIds),
                superUser,
                globalProjectAccess,
                true
        );
        return Optional.of(details);
    }

    public boolean hasPermission(Authentication authentication, String permissionKey) {
        AuthorizationUserDetails principal = getPrincipal(authentication);
        if (principal == null) {
            return false;
        }
        if (principal.superUser()) {
            return true;
        }
        return principal.permissions().contains(permissionKey);
    }

    public boolean hasProjectPermission(Authentication authentication, String permissionKey, Long projectId) {
        AuthorizationUserDetails principal = getPrincipal(authentication);
        if (principal == null) {
            return false;
        }
        if (principal.superUser()) {
            return true;
        }
        if (!principal.permissions().contains(permissionKey)) {
            return false;
        }
        if (projectId == null) {
            return true;
        }
        return principal.globalProjectAccess() || principal.projectIds().contains(projectId);
    }

    public boolean isSuperUser(Authentication authentication) {
        AuthorizationUserDetails principal = getPrincipal(authentication);
        return principal != null && principal.superUser();
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash service token", e);
        }
    }

    private AuthorizationUserDetails getPrincipal(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthorizationUserDetails authorizationUserDetails) {
            return authorizationUserDetails;
        }
        return null;
    }

    private <T> List<T> distinct(List<T> items) {
        Set<T> values = new LinkedHashSet<>();
        if (items != null) {
            values.addAll(items);
        }
        return List.copyOf(values);
    }
}
