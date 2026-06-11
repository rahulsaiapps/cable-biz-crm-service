package com.cablepulse.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Refuses to start in strict mode when the default development JWT secret is still configured.
 */
@Component
public class JwtSecretStartupValidator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(JwtSecretStartupValidator.class);

    static final String DEFAULT_DEV_SECRET = "cablepulse-local-dev-jwt-secret-key-32b-min";

    private final String jwtSecret;
    private final boolean strictSecrets;

    public JwtSecretStartupValidator(
            @Value("${cablepulse.jwt.secret}") String jwtSecret,
            @Value("${cablepulse.security.strict-secrets:false}") boolean strictSecrets) {
        this.jwtSecret = jwtSecret;
        this.strictSecrets = strictSecrets;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!strictSecrets) {
            if (DEFAULT_DEV_SECRET.equals(jwtSecret)) {
                logger.warn(
                        "Using default development JWT secret. "
                                + "Set CABLEPULSE_JWT_SECRET and CABLEPULSE_STRICT_SECRETS=true in production.");
            }
            return;
        }

        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("cablepulse.jwt.secret must be configured in strict mode");
        }
        if (DEFAULT_DEV_SECRET.equals(jwtSecret)) {
            throw new IllegalStateException(
                    "Refusing to start: default development JWT secret is not allowed when "
                            + "cablepulse.security.strict-secrets=true");
        }
        if (jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "Refusing to start: cablepulse.jwt.secret must be at least 32 bytes in strict mode");
        }
        logger.info("JWT secret validation passed (strict mode)");
    }
}
