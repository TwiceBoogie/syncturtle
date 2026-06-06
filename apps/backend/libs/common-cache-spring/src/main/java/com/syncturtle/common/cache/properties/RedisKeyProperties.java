package com.syncturtle.common.cache.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.redis.keys")
public final class RedisKeyProperties {

    private final String keyPrefix;

    public RedisKeyProperties(@DefaultValue("st:local:") String keyPrefix) {
        this.keyPrefix = normalizePrefix(keyPrefix, "key-prefix");
    }

    private static String normalizePrefix(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("app.redis.keys." + propertyName + " is required");
        }

        String trimmed = value.trim();

        if (trimmed.contains(" ")) {
            throw new IllegalArgumentException("app.redis.keys." + propertyName + " must not contain spaces");
        }

        return trimmed.endsWith(":") ? trimmed : trimmed + ":";
    }

}
