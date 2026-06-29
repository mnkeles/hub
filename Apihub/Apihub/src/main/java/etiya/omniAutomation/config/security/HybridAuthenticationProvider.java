package etiya.omniAutomation.config.security;

import etiya.omniAutomation.service.LdapUserService;
import etiya.omniAutomation.service.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class HybridAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(HybridAuthenticationProvider.class);
    private static final Pattern LDAP_INJECTION_PATTERN = Pattern.compile("[()\\\\*&|!<>=~]");
    private static final int MAX_USERNAME_LENGTH = 255;

    @Lazy
    @Autowired
    private UserServiceImpl userDetailsService;

    @Lazy
    @Autowired
    private LoginAttemptService loginAttemptService;

    @Lazy
    @Autowired
    private LdapUserService ldapUserService;

    private final PasswordEncoder passwordEncoder;

    public HybridAuthenticationProvider(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired(required = false)
    private LdapTemplate ldapTemplate;

    @Value("${ldap.enabled:false}")
    private boolean ldapEnabled;

    @Value("${ldap.user.dn.pattern:uid={0}}")
    private String userDnPattern;

    @Value("${ldap.group.search.base:}")
    private String groupSearchBase;

    @Value("${ldap.group.search.filter:(member={0})}")
    private String groupSearchFilter;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();
        String authType = "auto";
        if (authentication.getDetails() instanceof String) {
            authType = (String) authentication.getDetails();
        }

        if (loginAttemptService.isBlocked(username)) {
            logger.warn("Account is locked due to too many failed login attempts: {}", username);
            throw new LockedException("Account is temporarily locked. Please try again later.");
        }

        logger.info("=== LOGIN ATTEMPT === username: [{}], authType: [{}]", username, authType);
        try {
            Authentication auth;
            if ("local".equals(authType)) {
                auth = authenticateLocalOnly(username, password);
            } else if ("ldap".equals(authType)) {
                auth = authenticateLdapOnly(username, password);
            } else {
                auth = authenticateAuto(username, password);
            }
            loginAttemptService.loginSucceeded(username);
            logger.info("=== LOGIN SUCCESS === username: [{}]", username);
            return auth;
        } catch (LockedException e) {
            logger.warn("=== LOGIN FAILED (LOCKED) === username: [{}], reason: {}", username, e.getMessage());
            throw e;
        } catch (AuthenticationException e) {
            logger.warn("=== LOGIN FAILED === username: [{}], reason: {}", username, e.getMessage());
            loginAttemptService.loginFailed(username);
            throw e;
        } catch (Exception e) {
            logger.error("=== LOGIN ERROR === username: [{}], error: {}", username, e.getMessage(), e);
            loginAttemptService.loginFailed(username);
            throw new BadCredentialsException("Authentication error");
        }
    }

    private Authentication authenticateLocalOnly(String username, String password) {
        logger.debug("Local-only authentication for user: {}", username);
        UserDetails localUserDetails;
        try {
            localUserDetails = userDetailsService.loadUserByUsername(username);
        } catch (Exception e) {
            logger.warn("User not found in local database: {}", username);
            throw new BadCredentialsException("Invalid username or password");
        }
        return authenticateLocal(localUserDetails, password);
    }

    private Authentication authenticateLdapOnly(String username, String password) {
        logger.debug("LDAP-only authentication for user: {}", username);
        if (!ldapEnabled || ldapTemplate == null) {
            logger.warn("LDAP authentication attempted but LDAP is not enabled or configured");
            throw new BadCredentialsException("LDAP authentication is not available");
        }
        return authenticateLdap(username, password);
    }

    private Authentication authenticateAuto(String username, String password) {
        UserDetails localUserDetails = null;
        try {
            localUserDetails = userDetailsService.loadUserByUsername(username);
        } catch (Exception e) {
            logger.debug("User not found in local database: {}", username);
        }

        if (localUserDetails != null) {
            logger.debug("User found in local database, attempting local auth: {}", username);
            return authenticateLocal(localUserDetails, password);
        } else if (ldapEnabled && ldapTemplate != null) {
            logger.debug("User not in local database, attempting LDAP auth: {}", username);
            return authenticateLdap(username, password);
        } else {
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    private Authentication authenticateLocal(UserDetails userDetails, String password) {
        boolean matches = passwordEncoder.matches(password, userDetails.getPassword());
        if (matches) {
            logger.info("Local authentication successful for user: {}", userDetails.getUsername());
            return new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
        } else {
            logger.warn("Local authentication failed - invalid password for user: {}", userDetails.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    private Authentication authenticateLdap(String username, String password) {
        try {
            UserDetails ldapUser = authenticateWithLdap(username, password);
            ldapUserService.getActiveLdapUserOrThrow(ldapUser.getUsername());
            ldapUserService.updateLoginUpdateDate(ldapUser.getUsername());
            logger.info("LDAP authentication successful and ldap_user record found for user: {}", ldapUser.getUsername());
            return new UsernamePasswordAuthenticationToken(ldapUser, "", ldapUser.getAuthorities());
        } catch (BadCredentialsException e) {
            throw e;
        } catch (Exception e) {
            logger.error("LDAP authentication error for user: {}", username, e);
            throw new BadCredentialsException("Username or password hatalı");
        }
    }

    private UserDetails authenticateWithLdap(String username, String password) {
        validateUsername(username);

        String sanitizedUsername = sanitizeLdapInput(username);
        String samAccountName = ldapUserService.normalizeUsername(sanitizedUsername);

        AndFilter filter = new AndFilter();
        filter.and(new EqualsFilter("sAMAccountName", samAccountName));

        boolean authenticated = ldapTemplate.authenticate("", filter.encode(), password);

        if (!authenticated) {
            logger.debug("LDAP authentication failed for user: {}", username);
            throw new BadCredentialsException("Username or password hatalı");
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        return new User(samAccountName, "", true, true, true, true, authorities);
    }

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("Username exceeds maximum length");
        }
    }

    private String sanitizeLdapInput(String input) {
        if (input == null) {
            return "";
        }
        if (LDAP_INJECTION_PATTERN.matcher(input).find()) {
            logger.warn("Potential LDAP injection attempt detected in input: {}", input);
            throw new IllegalArgumentException("Invalid characters in input");
        }
        return input.trim();
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
