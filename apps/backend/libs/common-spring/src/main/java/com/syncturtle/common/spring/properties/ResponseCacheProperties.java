package com.syncturtle.common.spring.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.response-cache")
public final class ResponseCacheProperties {
    private final boolean enabled;
    private final String keyPrefix;
    private final int hashBytes;

    public ResponseCacheProperties(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("st:resp:") String keyPrefix,
            @DefaultValue("16") int hashBytes) {
        this.enabled = enabled;
        this.keyPrefix = requireText(keyPrefix, "key-prefix");
        this.hashBytes = requireRange(hashBytes, 8, 64, "hash-bytes");
    }

    private static String requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    "app.response-cache." + propertyName + " is required");
        }

        return value.trim();
    }

    private static int requireRange(int value, int min, int max, String propertyName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    "app.response-cache." + propertyName + " must be between " + min + " and " + max);
        }

        return value;
    }
}
