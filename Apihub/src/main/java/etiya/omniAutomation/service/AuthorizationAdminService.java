package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.PermissionDto;
import etiya.omniAutomation.business.dto.ServiceAccountDto;
import etiya.omniAutomation.business.dto.ServiceAccountPermissionAssignmentRequest;
import etiya.omniAutomation.business.dto.ServiceAccountRequest;
import etiya.omniAutomation.business.dto.ServiceTokenCreateRequest;
import etiya.omniAutomation.business.dto.ServiceTokenCreateResponse;
import etiya.omniAutomation.business.dto.UserPermissionAssignmentRequest;
import etiya.omniAutomation.business.dto.UserPermissionsResponseDto;
import etiya.omniAutomation.business.dto.UserSummaryDto;
import etiya.omniAutomation.entity.PermissionEntity;
import etiya.omniAutomation.entity.ProjectEntity;
import etiya.omniAutomation.entity.ServiceAccountEntity;
import etiya.omniAutomation.entity.ServiceAccountPermissionEntity;
import etiya.omniAutomation.entity.ServiceTokenEntity;
import etiya.omniAutomation.entity.UserEntity;
import etiya.omniAutomation.entity.UserPermissionEntity;
import etiya.omniAutomation.repository.PermissionRepository;
import etiya.omniAutomation.repository.ProjectRepository;
import etiya.omniAutomation.repository.ServiceAccountPermissionRepository;
import etiya.omniAutomation.repository.ServiceAccountRepository;
import etiya.omniAutomation.repository.ServiceTokenRepository;
import etiya.omniAutomation.repository.UserPermissionRepository;
import etiya.omniAutomation.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthorizationAdminService {

    private static final String SERVICE_TOKEN_PREFIX = "apihub_svc_";

    private final PermissionRepository permissionRepository;
    private final ServiceAccountRepository serviceAccountRepository;
    private final ServiceAccountPermissionRepository serviceAccountPermissionRepository;
    private final ServiceTokenRepository serviceTokenRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final AuthorizationPermissionService authorizationPermissionService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Cacheable(value = "authCatalog", key = "'permissions'")
    public List<PermissionDto> getPermissions() {
        return permissionRepository.findAll().stream()
                .map(e -> new PermissionDto(e.getPermissionId(), e.getPermissionKey(), e.getName(),
                        e.getDescription(), e.getCategory(), e.getUiVisible(), e.getServiceAssignable(), e.getEnabled()))
                .toList();
    }

    @Cacheable(value = "authCatalog", key = "'users'")
    public List<UserSummaryDto> getUsers() {
        return userRepository.findAll().stream()
                .map(u -> new UserSummaryDto(u.getUserId(), u.getEmail(), u.getFirstName(), u.getLastName(), u.getAuthType(), u.getEnabled()))
                .toList();
    }

    public UserPermissionsResponseDto getUserPermissions(Long userId) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        List<UserPermissionEntity> userPermissions = userPermissionRepository.findByUserId(userId);
        List<PermissionDto> permissions = userPermissions.stream()
                .map(up -> {
                    PermissionEntity p = up.getPermissionEntity();
                    return new PermissionDto(p.getPermissionId(), p.getPermissionKey(), p.getName(),
                            p.getDescription(), p.getCategory(), p.getUiVisible(), p.getServiceAssignable(), p.getEnabled());
                })
                .toList();
        return new UserPermissionsResponseDto(user.getUserId(), user.getEmail(), permissions);
    }

    @Cacheable(value = "authCatalog", key = "'serviceAccounts'")
    public List<ServiceAccountDto> getServiceAccounts() {
        return serviceAccountRepository.findAll().stream()
                .map(e -> new ServiceAccountDto(e.getServiceAccountId(), e.getServiceCode(),
                        e.getName(), e.getDescription(), e.getOwner(), e.getEnabled()))
                .toList();
    }

    @CacheEvict(value = "authCatalog", key = "'serviceAccounts'")
    public ServiceAccountDto createServiceAccount(ServiceAccountRequest request) {
        ServiceAccountEntity entity = new ServiceAccountEntity();
        entity.setServiceCode(request.serviceCode());
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setOwner(request.owner());
        entity.setEnabled(1);
        ServiceAccountEntity saved = serviceAccountRepository.save(entity);
        return new ServiceAccountDto(saved.getServiceAccountId(), saved.getServiceCode(),
                saved.getName(), saved.getDescription(), saved.getOwner(), saved.getEnabled());
    }

    @Transactional
    @CacheEvict(value = {"authCatalog", "authUserDetails"}, allEntries = true)
    public void assignUserPermissions(Long userId, UserPermissionAssignmentRequest request) {
        UserEntity user = userRepository.findById(userId).orElseThrow();
        userPermissionRepository.deleteByUserId(userId);
        if (request.permissionIds() == null) {
            return;
        }
        List<Long> permissionIds = request.permissionIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, PermissionEntity> permissionMap = permissionRepository.findAllById(permissionIds).stream()
                .collect(Collectors.toMap(PermissionEntity::getPermissionId, Function.identity()));
        List<UserPermissionEntity> assignments = permissionIds.stream()
                .map(permissionMap::get)
                .filter(Objects::nonNull)
                .map(permission -> {
                    UserPermissionEntity entity = new UserPermissionEntity();
                    entity.setUserEntity(user);
                    entity.setPermissionEntity(permission);
                    return entity;
                })
                .toList();
        userPermissionRepository.saveAll(assignments);
    }

    @Transactional
    @CacheEvict(value = {"authCatalog", "authUserDetails"}, allEntries = true)
    public void assignServiceAccountPermissions(Long serviceAccountId, ServiceAccountPermissionAssignmentRequest request) {
        ServiceAccountEntity serviceAccount = serviceAccountRepository.findById(serviceAccountId).orElseThrow();
        serviceAccountPermissionRepository.deleteByServiceAccountId(serviceAccountId);
        if (request.assignments() == null) {
            return;
        }
        List<ServiceAccountPermissionAssignmentRequest.PermissionProjectAssignment> assignmentsInput = request.assignments().stream()
                .filter(Objects::nonNull)
                .toList();
        List<Long> permissionIds = assignmentsInput.stream()
                .map(ServiceAccountPermissionAssignmentRequest.PermissionProjectAssignment::permissionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> projectIds = assignmentsInput.stream()
                .map(ServiceAccountPermissionAssignmentRequest.PermissionProjectAssignment::projectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, PermissionEntity> permissionMap = permissionRepository.findAllById(permissionIds).stream()
                .collect(Collectors.toMap(PermissionEntity::getPermissionId, Function.identity()));
        Map<Long, ProjectEntity> projectMap = projectRepository.findAllById(projectIds).stream()
                .collect(Collectors.toMap(ProjectEntity::getProjectId, Function.identity()));
        List<ServiceAccountPermissionEntity> assignments = assignmentsInput.stream()
                .map(assignment -> buildServiceAccountPermission(serviceAccount, assignment, permissionMap, projectMap))
                .filter(Objects::nonNull)
                .toList();
        serviceAccountPermissionRepository.saveAll(assignments);
    }

    @Transactional
    public ServiceTokenCreateResponse createServiceToken(Long serviceAccountId, ServiceTokenCreateRequest request, Authentication authentication) {
        ServiceAccountEntity serviceAccount = serviceAccountRepository.findById(serviceAccountId).orElseThrow();
        String token = SERVICE_TOKEN_PREFIX + randomTokenSecret();
        String tokenHash = authorizationPermissionService.hashToken(token);
        String tokenPrefix = token.substring(0, Math.min(30, token.length()));

        ServiceTokenEntity entity = new ServiceTokenEntity();
        entity.setServiceAccountEntity(serviceAccount);
        entity.setTokenName(request.tokenName());
        entity.setTokenHash(tokenHash);
        entity.setTokenPrefix(tokenPrefix);
        entity.setCreatedBy(authentication == null ? null : authentication.getName());
        if (request.expiresAt() != null) {
            entity.setExpiresAt(Date.from(request.expiresAt()));
        }

        ServiceTokenEntity saved = serviceTokenRepository.save(entity);
        Instant expiresAt = saved.getExpiresAt() == null ? null : saved.getExpiresAt().toInstant();
        return new ServiceTokenCreateResponse(saved.getServiceTokenId(), saved.getTokenName(), token, saved.getTokenPrefix(), expiresAt);
    }

    @Transactional
    @CacheEvict(value = "authUserDetails", allEntries = true)
    public void revokeServiceToken(Long tokenId) {
        ServiceTokenEntity token = serviceTokenRepository.findById(tokenId).orElseThrow();
        token.setRevokedAt(new Date());
        serviceTokenRepository.save(token);
    }

    private ServiceAccountPermissionEntity buildServiceAccountPermission(ServiceAccountEntity serviceAccount,
                                                                         ServiceAccountPermissionAssignmentRequest.PermissionProjectAssignment assignment,
                                                                         Map<Long, PermissionEntity> permissionMap,
                                                                         Map<Long, ProjectEntity> projectMap) {
        PermissionEntity permission = permissionMap.get(assignment.permissionId());
        if (permission == null) {
            return null;
        }
        ProjectEntity project = assignment.projectId() == null ? null : projectMap.get(assignment.projectId());
        if (assignment.projectId() != null && project == null) {
            return null;
        }
        ServiceAccountPermissionEntity entity = new ServiceAccountPermissionEntity();
        entity.setServiceAccountEntity(serviceAccount);
        entity.setPermissionEntity(permission);
        entity.setProjectEntity(project);
        return entity;
    }

    private String randomTokenSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
