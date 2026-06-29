package etiya.omniAutomation.config.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

@Configuration
public class RsaKeyConfig {

    private static final Logger logger = LoggerFactory.getLogger(RsaKeyConfig.class);
    private static final int KEY_SIZE = 2048;

    @Bean
    public KeyPair rsaKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(KEY_SIZE);
            KeyPair keyPair = kpg.generateKeyPair();
            logger.info("RSA KeyPair generated successfully ({} bit)", KEY_SIZE);
            return keyPair;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        }
    }
}
