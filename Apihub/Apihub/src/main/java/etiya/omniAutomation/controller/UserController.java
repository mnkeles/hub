package etiya.omniAutomation.controller;

import etiya.omniAutomation.business.dto.CurrentUserResponse;
import etiya.omniAutomation.business.dto.UserCreateDto;
import etiya.omniAutomation.config.security.JwtUtils;
import etiya.omniAutomation.entity.LdapUserEntity;
import etiya.omniAutomation.entity.UserEntity;
import etiya.omniAutomation.service.LdapUserService;
import etiya.omniAutomation.service.UserServiceImpl;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RolesAllowed("ROLE_USER")
@RequiredArgsConstructor
public class UserController {
    private final UserServiceImpl userService;
    private final LdapUserService ldapUserService;
    private final JwtUtils jwtUtils;

    @GetMapping("/current")
    public ResponseEntity<CurrentUserResponse> getCurrentUser(HttpServletRequest request) {
        String token = extractBearerToken(request);
        String username = jwtUtils.extractUsername(token);
        String authType = jwtUtils.extractAuthType(token);

        if ("ldap".equalsIgnoreCase(authType)) {
            LdapUserEntity ldapUser = ldapUserService.getActiveLdapUserOrThrow(username);
            return ResponseEntity.ok(new CurrentUserResponse(
                    ldapUser.getUsername(),
                    "ldap",
                    ldapUser.getFirstName(),
                    ldapUser.getLastName(),
                    ldapUser.getEnabled(),
                    ldapUser.getProjectId()
            ));
        }

        return userService.getUserByAnyEmail(username)
                .map(user -> ResponseEntity.ok(new CurrentUserResponse(
                        user.getEmail(),
                        "local",
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEnabled(),
                        user.getProjectId()
                )))
                .orElseGet(() -> {
                    LdapUserEntity ldapUser = ldapUserService.getActiveLdapUserOrThrow(username);
                    return ResponseEntity.ok(new CurrentUserResponse(
                            ldapUser.getUsername(),
                            "ldap",
                            ldapUser.getFirstName(),
                            ldapUser.getLastName(),
                            ldapUser.getEnabled(),
                            ldapUser.getProjectId()
                    ));
                });
    }

    @GetMapping("/create")
    public ResponseEntity<Void> createUser(UserCreateDto userCreateDto) {
        UserEntity userEntity = new UserEntity();
        userEntity.setFirstName(userCreateDto.getFirstName());
        userEntity.setLastName(userCreateDto.getLastName());
        userEntity.setEmail(userCreateDto.getEmail());
        userEntity.setPassword(userCreateDto.getPassword());
        userEntity.setProjectId(userCreateDto.getProjectId());
        userService.save(userEntity);
        return ResponseEntity.ok().build();
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadCredentialsException("Authorization token is required");
        }
        return authHeader.substring(7);
    }
}
