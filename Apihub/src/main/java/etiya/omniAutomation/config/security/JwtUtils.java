package etiya.omniAutomation.config.security;

import etiya.omniAutomation.business.dto.AuthResponse;
import etiya.omniAutomation.entity.RefreshTokenEntity;
import etiya.omniAutomation.entity.UserEntity;
import etiya.omniAutomation.repository.RefreshTokenRepository;
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

    public AuthResponse generateJwtTokenForLdapUser(String username) {
        long now = System.currentTimeMillis();
        String accessToken = Jwts.builder()
                .signWith(secretKeySpec)
                .expiration(new Date(now + expiration))
                .issuedAt(new Date(now))
                .subject(username)
                .claim("authType", "ldap")
                .issuer(issuer)
                .audience()
                .add(issuer + issuer)
                .and()
                .compact();
        String refreshToken = Jwts.builder()
                .signWith(secretKeySpec)
                .subject(username)
                .claim("authType", "ldap")
                .issuer(issuer)
                .audience()
                .add(issuer + issuer)
                .and()
                .compact();
        return new AuthResponse(accessToken, refreshToken);
    }

    public AuthResponse generateJwtToken(UserEntity userEntity) {
        long now = System.currentTimeMillis();
        String accessToken = Jwts.builder()
                .signWith(secretKeySpec)
                .expiration(new Date(now + expiration))
                .issuedAt(new Date(now))
                .subject(userEntity.getEmail())
                .claim("authType", "local")
                .issuer(issuer)
                .audience()
                .add(issuer + issuer)
                .and()
                .compact();
        Date refreshExp = new Date(now + refreshExpiration);
        String refreshToken = Jwts.builder()
                .signWith(secretKeySpec)
                .subject(userEntity.getEmail())
                .claim("authType", "local")
                .issuer(issuer)
                .audience()
                .add(issuer + issuer)
                .and()
                .compact();
        this.createRefreshToken(userEntity.getUserId(), refreshToken, refreshExp);
        return new AuthResponse(accessToken, refreshToken);
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
        return Jwts.parser().verifyWith(secretKeySpec)
                .requireIssuer(issuer)
                .requireAudience(issuer + issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String extractAuthType(String token) {
        Object authType = Jwts.parser().verifyWith(secretKeySpec)
                .requireIssuer(issuer)
                .requireAudience(issuer + issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("authType");
        return authType == null ? "local" : authType.toString();
    }
}
