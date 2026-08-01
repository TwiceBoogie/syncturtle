package com.syncturtle.platform.gateway.configuration.property;

import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.passport.gateway", ignoreUnknownFields = false)
public final class GatewayPassportProperties {
    private final String issuer;
    private final String audience;
    private final String authHeaderPrefix;
    private final Redis redis;

    public GatewayPassportProperties(
            String issuer,
            String audience,
            @DefaultValue("X-Auth-") String authHeaderPrefix,
            @DefaultValue Redis redis) {
        this.issuer = requireText(issuer, "issuer");
        this.audience = requireText(audience, "audience");
        this.authHeaderPrefix = requireText(authHeaderPrefix, "auth-header-prefix");
        this.redis = Objects.requireNonNull(redis, "redis is required");
    }

    @Getter
    public static final class Redis {
        private final String sessionKeyPrefix;
        private final String userVersionKeyPrefix;
        private final String adminSessionVersionKeyPrefix;

        public Redis(
                @DefaultValue("auth:session:") String sessionKeyPrefix,
                @DefaultValue("auth:user-version:") String userVersionKeyPrefix,
                @DefaultValue("auth:admin-session-version:") String adminSessionVersionKeyPrefix) {
            this.sessionKeyPrefix = requireText(sessionKeyPrefix, "redis.session-key-prefix");
            this.userVersionKeyPrefix = requireText(userVersionKeyPrefix, "redis.user-version-key-prefix");
            this.adminSessionVersionKeyPrefix = requireText(adminSessionVersionKeyPrefix,
                    "redis-admin-session-version-key-prefix");
        }
    }

    private static String requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    "app.passport.gateway." + propertyName + " is required");
        }

        return value.trim();
    }
}
