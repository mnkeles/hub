package etiya.omniAutomation.config.security;

import etiya.omniAutomation.service.AuthorizationPermissionService;
import etiya.omniAutomation.service.UserServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityFilter extends OncePerRequestFilter {

    private static final String SERVICE_TOKEN_PREFIX = "apihub_svc_";

    private final JwtUtils jwtUtils;
    private final UserServiceImpl userDetailsService;
    private final AuthorizationPermissionService authorizationPermissionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String token = authHeader.substring(7);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null && token.startsWith(SERVICE_TOKEN_PREFIX)) {
                Optional<AuthorizationUserDetails> servicePrincipal = authorizationPermissionService.authenticateServiceToken(token);
                if (servicePrincipal.isPresent()) {
                    setAuthentication(request, servicePrincipal.get());
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            final String userEmail = jwtUtils.extractUsername(token);

            if (userEmail != null && authentication == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                if (jwtUtils.validateJwtToken(token, userDetails)) {
                    setAuthentication(request, userDetails);
                }
            }
        } catch (Exception e) {
            log.error("Authentication validation error: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/auth/") ||
               path.startsWith("/api/callStep/") ||
               path.startsWith("/api/callProcess/") ||
               path.equals("/api/chat/health") ||
               path.equals("/health") ||
               path.equals("/error");
    }
}
