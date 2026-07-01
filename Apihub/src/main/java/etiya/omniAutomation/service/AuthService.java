package etiya.omniAutomation.service;

import etiya.omniAutomation.business.dto.AuthResponse;
import etiya.omniAutomation.config.security.JwtUtils;
import etiya.omniAutomation.entity.UserEntity;
import etiya.omniAutomation.request.AuthRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final JwtUtils jwtUtils;
    private final UserServiceImpl userService;
    private final AuthenticationManager authenticationManager;
    private final KeyPair rsaKeyPair;
    private final LdapUserService ldapUserService;

    public AuthResponse login(AuthRequest authRequest) {
        String decryptedPassword = decryptPassword(authRequest.getPassword());
        String normalizedUsername = normalizeUsername(authRequest.getUsername(), authRequest.getAuthType());

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(normalizedUsername, decryptedPassword);
        authToken.setDetails(authRequest.getAuthType());

        authenticationManager.authenticate(authToken);

        if ("ldap".equalsIgnoreCase(authRequest.getAuthType())) {
            logger.info("LDAP user authenticated, generating token without DB lookup: {}", normalizedUsername);
            return jwtUtils.generateJwtTokenForLdapUser(normalizedUsername);
        }

        UserEntity userEntity = this.userService.getUserByEmail(normalizedUsername)
                .orElseThrow(() -> new SecurityException("User not found"));

        return jwtUtils.generateJwtToken(userEntity);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        String email = jwtUtils.extractUsername(refreshToken);
        String authType = jwtUtils.extractAuthType(refreshToken);

        if ("ldap".equalsIgnoreCase(authType)) {
            ldapUserService.getActiveLdapUserOrThrow(email);
            return jwtUtils.generateJwtTokenForLdapUser(email);
        }

        return this.userService.getUserByEmail(email)
                .map(jwtUtils::generateJwtToken)
                .orElseGet(() -> {
                    ldapUserService.getActiveLdapUserOrThrow(email);
                    return jwtUtils.generateJwtTokenForLdapUser(email);
                });
    }

    private String decryptPassword(String password) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(password);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
            String decrypted = new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
            logger.debug("Password decrypted successfully via RSA");
            return decrypted;
        } catch (Exception e) {
            logger.warn("RSA decryption failed ({}), using password as plaintext", e.getMessage());
            return password;
        }
    }

    private String normalizeUsername(String username, String authType) {
        if (!"ldap".equalsIgnoreCase(authType) || username == null) {
            return username;
        }
        return ldapUserService.normalizeUsername(username);
    }
}
