package com.syncturtle.services.email.configuration.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.email.runtime-config-cache", ignoreUnknownFields = false)
public final class EmailRuntimeConfigCacheProperties {

    private final String name;
    private final Duration ttl;

    public EmailRuntimeConfigCacheProperties(String name, Duration ttl) {
        Assert.hasText(name, "app.email.runtime-config-cache.name is required");
        requirePositiveDuration(ttl, "app.email.runtime-config-cache.ttl");

        this.name = name.trim();
        this.ttl = ttl;
    }

    private static void requirePositiveDuration(Duration duration, String propertyName) {
        Assert.notNull(duration, propertyName + " is required");
        Assert.isTrue(!duration.isNegative() && !duration.isZero(), propertyName + " must be postive");
    }

}
