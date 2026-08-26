package com.syncturtle.services.user.configuration.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;

/**
 * Configuration for authentication and OAuth
 */
@Getter
@ConfigurationProperties(prefix = "app.auth.token", ignoreUnknownFields = false)
public final class AuthProperties {

    private static final int MIN_PASSWORD_RESET_TOKEN_BYTES = 32;
    private static final int MAX_PASSWORD_RESET_TOKEN_BYTES = 128;

    /**
     * Expiration time for access tokens in minutes
     */
    private final Duration accessTokenTtl;

    private final Duration passwordResetTokenTtl;
    private final int passwordResetTokenBytes;

    public AuthProperties(
            Duration accessTokenTtl,
            Duration passwordResetTokenTtl,
            int passwordResetTokenBytes) {
        this.accessTokenTtl = requirePositive(accessTokenTtl, "access-token-ttl");
        this.passwordResetTokenTtl = requirePositive(passwordResetTokenTtl, "password-reset-token-ttl");
        if (passwordResetTokenBytes < MIN_PASSWORD_RESET_TOKEN_BYTES
                || passwordResetTokenBytes > MAX_PASSWORD_RESET_TOKEN_BYTES) {
            throw new IllegalArgumentException("app.auth.token.password-reset-token-bytes must be between "
                    + MIN_PASSWORD_RESET_TOKEN_BYTES
                    + " and "
                    + MAX_PASSWORD_RESET_TOKEN_BYTES);
        }

        this.passwordResetTokenBytes = passwordResetTokenBytes;
    }

    public long accessTokenMaxAgeSeconds() {
        return accessTokenTtl.getSeconds();
    }

    private static Duration requirePositive(Duration value, String propertyName) {
        if (value == null) {
            throw new IllegalArgumentException("app.auth.token." + propertyName + " is required");
        }
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("app.auth.token." + propertyName + " must be positive");
        }
        return value;
    }

}
