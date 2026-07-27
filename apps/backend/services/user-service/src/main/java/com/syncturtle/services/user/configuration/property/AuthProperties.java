package com.syncturtle.services.user.configuration.property;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import lombok.Getter;

/**
 * Configuration for authentication and OAuth
 */
@Getter
@ConfigurationProperties(prefix = "app.auth")
public final class AuthProperties {

    private static final int MIN_PASSWORD_RESET_TOKEN_BYTES = 32;
    private static final int MAX_PASSWORD_RESET_TOKEN_BYTES = 128;

    /**
     * Expiration time for access tokens in minutes
     */
    private final Duration accessTokenTtl;

    /**
     * Expiration time for refresh tokens in days
     */
    private final Duration refreshTokenTtl;
    private final Duration passwordResetTokenTtl;
    private final int passwordResetTokenBytes;

    public AuthProperties(
            @DefaultValue("30m") Duration accessTokenTtl,
            @DefaultValue("30d") Duration refreshTokenTtl,
            @DefaultValue("30m") Duration passwordResetTokenTtl,
            @DefaultValue("48") int passwordResetTokenBytes) {
        this.accessTokenTtl = Objects.requireNonNull(accessTokenTtl, "accessTokenTtl is required");
        this.refreshTokenTtl = Objects.requireNonNull(refreshTokenTtl, "refreshTokenTtl is required");
        this.passwordResetTokenTtl = Objects.requireNonNull(passwordResetTokenTtl, "passwordResetTokenTtl is required");

        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("accessTokenTtl must be positive");
        }
        if (refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalArgumentException("refreshTokenTtl must be positive");
        }
        if (passwordResetTokenTtl.isZero() || passwordResetTokenTtl.isNegative()) {
            throw new IllegalArgumentException("passwordResetTokenTtl must be positive");
        }
        if (passwordResetTokenBytes < MIN_PASSWORD_RESET_TOKEN_BYTES
                || passwordResetTokenBytes > MAX_PASSWORD_RESET_TOKEN_BYTES) {
            throw new IllegalArgumentException("passwordResetTokenBytes must be between "
                    + MIN_PASSWORD_RESET_TOKEN_BYTES
                    + " and "
                    + MAX_PASSWORD_RESET_TOKEN_BYTES);
        }

        this.passwordResetTokenBytes = passwordResetTokenBytes;
    }

    public long accessTokenMaxAgeSeconds() {
        return accessTokenTtl.getSeconds();
    }

    public long refreshTokenMaxAgeSeconds() {
        return refreshTokenTtl.getSeconds();
    }

}
