package com.syncturtle.services.email.configuration.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.email.runtime-config-cache", ignoreUnknownFields = false)
public final class EmailRuntimeConfigCacheProperties {

    private final Duration ttl;

    public EmailRuntimeConfigCacheProperties(Duration ttl) {
        this.ttl = requirePositiveDuration(ttl, "app.email.runtime-config-cache.ttl");
    }

    private static Duration requirePositiveDuration(Duration duration, String propertyName) {
        Assert.notNull(duration, propertyName + " is required");
        Assert.isTrue(!duration.isNegative() && !duration.isZero(), propertyName + " must be positive");

        return duration;
    }

}
