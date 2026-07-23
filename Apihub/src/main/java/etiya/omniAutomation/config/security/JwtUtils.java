package etiya.omniAutomation.config.security;

import etiya.omniAutomation.business.dto.AuthResponse;
import etiya.omniAutomation.business.dto.AuthUserResponse;
import etiya.omniAutomation.entity.RefreshTokenEntity;
import etiya.omniAutomation.entity.UserEntity;
import etiya.omniAutomation.repository.RefreshTokenRepository;
import etiya.omniAutomation.service.AuthorizationPermissionService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.auth.secret}")
    private String secretKey;

    @Value("${jwt.auth.issuer}")
    private String issuer;

    @Value("${jwt.auth.expiration}")
    private Long expiration;

    @Value("${jwt.auth.refresh_expiration}")
    private Long refreshExpiration;
    private SecretKey secretKeySpec;

    @PostConstruct
    public void init() {
        secretKeySpec = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthorizationPermissionService authorizationPermissionService;

    public AuthResponse generateJwtToken(UserEntity userEntity) {
        AuthorizationUserDetails userDetails = authorizationPermissionService.loadUserDetails(userEntity.getEmail());
        long now = System.currentTimeMillis();
        String accessToken = buildToken(userDetails, now, expiration);
        Date refreshExp = new Date(now + refreshExpiration);
        String refreshToken = buildRefreshToken(userDetails, now);
        this.createRefreshToken(userEntity.getUserId(), refreshToken, refreshExp);
        String authType = "LDAP_USER".equals(userDetails.principalType()) ? "ldap" : "local";
        return new AuthResponse(accessToken, refreshToken, toAuthUserResponse(userDetails, authType));
    }

    private String buildToken(AuthorizationUserDetails userDetails, long now, long tokenExpiration) {
        return Jwts.builder()
                .signWith(secretKeySpec)
                .expiration(new Date(now + tokenExpiration))
                .issuedAt(new Date(now))
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.principalId())
                .claim("authType", "LDAP_USER".equals(userDetails.principalType()) ? "ldap" : "local")
                .claim("principalType", userDetails.principalType())
                .claim("permissions", userDetails.permissions())
                .claim("projectIds", userDetails.projectIds())
                .claim("superUser", userDetails.superUser())
                .issuer(issuer)
                .audience()
                .add(issuer + issuer)
                .and()
                .compact();
    }

    private String buildRefreshToken(AuthorizationUserDetails userDetails, long now) {
        return Jwts.builder()
                .signWith(secretKeySpec)
                .issuedAt(new Date(now))
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.principalId())
                .claim("authType", "LDAP_USER".equals(userDetails.principalType()) ? "ldap" : "local")
                .claim("principalType", userDetails.principalType())
                .issuer(issuer)
                .audience()
                .add(issuer + issuer)
                .and()
                .compact();
    }

    public boolean validateJwtToken(String authToken, UserDetails userDetails) {
        try {
            JwtParser build = Jwts.parser().verifyWith(secretKeySpec)
                    .requireIssuer(issuer)
                    .requireAudience(issuer + issuer)
                    .requireSubject(userDetails.getUsername())
                    .build();
            build.parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }

    public void createRefreshToken(Long userId, String refreshToken, Date expiryDate) {
        var token = refreshTokenRepository.findByUserId(userId).orElse(new RefreshTokenEntity());
        token.setUserId(userId);
        token.setExpiryDate(expiryDate.toInstant());
        token.setToken(refreshToken);
        refreshTokenRepository.save(token);
    }

    public boolean isTokenExpired(Long userId, String token) {
        return refreshTokenRepository.isTokenExpired(userId, token, Instant.now());
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractAuthType(String token) {
        Object authType = parseClaims(token).get("authType");
        return authType == null ? "local" : authType.toString();
    }

    public Long extractUserId(String token) {
        Object userId = parseClaims(token).get("userId");
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKeySpec)
                .requireIssuer(issuer)
                .requireAudience(issuer + issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private AuthUserResponse toAuthUserResponse(AuthorizationUserDetails userDetails, String authType) {
        return new AuthUserResponse(
                userDetails.principalId() == null ? null : userDetails.principalId().toString(),
                userDetails.username(),
                "local".equals(authType) ? userDetails.username() : null,
                authType,
                userDetails.permissions()
        );
    }
}
