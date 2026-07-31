package com.syncturtle.common.cache.property;

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
    private final String bodyTypeAllowlistPrefix;
    private final String workspaceHeaderName;

    public ResponseCacheProperties(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("st:local:rc:") String keyPrefix,
            @DefaultValue("16") int hashBytes,
            @DefaultValue("com.syncturtle.") String bodyTypeAllowlistPrefix,
            @DefaultValue("X-Workspace-Id") String workspaceHeaderName) {
        this.enabled = enabled;
        this.keyPrefix = normalizePrefix(keyPrefix, "key-prefix");
        this.hashBytes = requireRange(hashBytes, 8, 32, "hash-bytes");
        this.bodyTypeAllowlistPrefix = requireText(bodyTypeAllowlistPrefix, "body-type-allowlist-prefix");
        this.workspaceHeaderName = requireText(workspaceHeaderName, "workspace-header-name");
    }

    private static String normalizePrefix(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("app.response-cache." + propertyName + " is required");
        }

        String trimmed = value.trim();

        if (trimmed.contains(" ")) {
            throw new IllegalArgumentException("app.response-cache." + propertyName + " must not contain spaces");
        }

        return trimmed.endsWith(":") ? trimmed : trimmed + ":";
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
