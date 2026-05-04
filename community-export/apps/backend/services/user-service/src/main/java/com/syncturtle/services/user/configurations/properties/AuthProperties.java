package com.syncturtle.services.user.configurations.properties;

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

    /**
     * Expiration time for access tokens in minutes
     */
    private final Duration accessTokenTtl;

    /**
     * Expiration time for refresh tokens in days
     */
    private final Duration refreshTokenTtl;

    public AuthProperties(
            @DefaultValue("15m") Duration accessTokenTtl,
            @DefaultValue("30d") Duration refreshTokenTtl) {
        this.accessTokenTtl = Objects.requireNonNull(accessTokenTtl, "accessTokenTtl is required");
        this.refreshTokenTtl = Objects.requireNonNull(refreshTokenTtl, "refreshTokenTtl is required");

        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("accessTokenTtl must be positive");
        }
        if (refreshTokenTtl.isZero() || refreshTokenTtl.isNegative()) {
            throw new IllegalArgumentException("refreshTokenTtl must be positive");
        }
    }

    public long accessTokenMaxAgeSeconds() {
        return accessTokenTtl.getSeconds();
    }

    public long refreshTokenMaxAgeSeconds() {
        return refreshTokenTtl.getSeconds();
    }

}
