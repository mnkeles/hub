package etiya.omniAutomation.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "ldap.enabled", havingValue = "true")
public class LdapConfig {

    private static final Logger logger = LoggerFactory.getLogger(LdapConfig.class);

    @Value("${ldap.url}")
    private String ldapUrl;

    @Value("${ldap.base}")
    private String ldapBase;

    @Value("${ldap.username:}")
    private String ldapUsername;

    @Value("${ldap.password:}")
    private String ldapPassword;

    @Value("${ldap.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${ldap.connection.timeout:10000}")
    private String connectionTimeout;

    @Value("${ldap.read.timeout:10000}")
    private String readTimeout;

    @Bean
    public LdapContextSource ldapContextSource() {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrl(ldapUrl);
        contextSource.setBase(ldapBase);

        if (ldapUsername != null && !ldapUsername.isBlank()) {
            contextSource.setUserDn(ldapUsername);
            contextSource.setPassword(ldapPassword);
        }

        Map<String, Object> baseEnv = new HashMap<>();
        baseEnv.put("com.sun.jndi.ldap.connect.timeout", connectionTimeout);
        baseEnv.put("com.sun.jndi.ldap.read.timeout", readTimeout);
        contextSource.setBaseEnvironmentProperties(baseEnv);

        contextSource.setPooled(true);

        if (!sslEnabled) {
            contextSource.setUrl(ldapUrl.replace("ldaps://", "ldap://"));
        }

        contextSource.afterPropertiesSet();
        logger.info("LDAP Context Source configured: url={}, base={}, ssl={}", ldapUrl, ldapBase, sslEnabled);
        return contextSource;
    }

    @Bean
    public LdapTemplate ldapTemplate(LdapContextSource ldapContextSource) {
        LdapTemplate template = new LdapTemplate(ldapContextSource);
        template.setIgnorePartialResultException(true);
        return template;
    }
}
