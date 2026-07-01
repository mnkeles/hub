package etiya.omniAutomation.service;

import etiya.omniAutomation.entity.LdapUserEntity;
import etiya.omniAutomation.repository.LdapUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LdapUserService {

    private static final String ADMIN_MESSAGE = "Apihub Admin ile görüşünüz";
    private static final String DEFAULT_ROLE = "ROLE_USER";

    private final LdapUserRepository ldapUserRepository;

    public String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String normalizedUsername = username.trim();
        int atIndex = normalizedUsername.indexOf('@');
        if (atIndex > 0) {
            return normalizedUsername.substring(0, atIndex);
        }
        return normalizedUsername;
    }

    public LdapUserEntity getActiveLdapUserOrThrow(String username) {
        String normalizedUsername = normalizeUsername(username);
        LdapUserEntity ldapUser = ldapUserRepository.findByUsernameAndEnabled(normalizedUsername, 1);
        if (ldapUser == null) {
            throw new BadCredentialsException(ADMIN_MESSAGE);
        }
        return ldapUser;
    }

    public UserDetails loadLdapUserDetails(String username) {
        LdapUserEntity ldapUser = getActiveLdapUserOrThrow(username);
        return new User(
                ldapUser.getUsername(),
                "",
                true,
                true,
                true,
                true,
                List.of(new SimpleGrantedAuthority(DEFAULT_ROLE))
        );
    }

    @Transactional
    public void updateLoginUpdateDate(String username) {
        LdapUserEntity ldapUser = getActiveLdapUserOrThrow(username);
        ldapUser.setUdate(new Date());
        ldapUserRepository.save(ldapUser);
    }
}
