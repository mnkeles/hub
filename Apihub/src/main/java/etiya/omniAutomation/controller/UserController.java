package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.CurrentUserResponse;
import etiya.omniAutomation.business.dto.UserCreateDto;
import etiya.omniAutomation.common.PermissionConstants;
import etiya.omniAutomation.config.security.AuthorizationUserDetails;
import etiya.omniAutomation.entity.UserEntity;
import etiya.omniAutomation.service.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {
    private final UserServiceImpl userService;

    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthorizationUserDetails currentUser) {
            if ("SERVICE_ACCOUNT".equals(currentUser.principalType())) {
                return ResponseEntity.ok(new CurrentUserResponse(
                        currentUser.username(),
                        "service",
                        currentUser.username(),
                        null,
                        currentUser.enabled() ? 1 : 0,
                        currentUser.permissions()
                ));
            }

            return userService.getUserByAnyEmail(currentUser.username())
                    .map(user -> ResponseEntity.ok(new CurrentUserResponse(
                            user.getEmail(),
                            "LDAP".equals(user.getAuthType()) ? "ldap" : "local",
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEnabled(),
                            currentUser.permissions()
                    )))
                    .orElseGet(() -> ResponseEntity.status(404).build());
        }
        return ResponseEntity.status(401).build();
    }

    @GetMapping("/create")
    @PreAuthorize("@authorizationPermissionService.hasPermission(authentication, '" + PermissionConstants.USER_MANAGE + "')")
    public ResponseEntity<Void> createUser(UserCreateDto userCreateDto) {
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstName(userCreateDto.getFirstName());
        userEntity.setLastName(userCreateDto.getLastName());
        userEntity.setEmail(userCreateDto.getEmail());
        userEntity.setPassword(userCreateDto.getPassword());
        userService.save(userEntity);
        return ResponseEntity.ok().build();
    }
}
