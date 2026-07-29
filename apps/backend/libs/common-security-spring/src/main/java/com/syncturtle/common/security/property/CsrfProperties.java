package com.syncturtle.common.security.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.security.csrf")
public final class CsrfProperties {

    private static final int MIN_RAW_TOKEN_BYTES = 16;

    private final boolean enabled;
    private final String signingKey;
    private final int rawTokenBytes;

    public CsrfProperties(
            @DefaultValue("true") boolean enabled,
            String signingKey,
            @DefaultValue("32") int rawTokenBytes) {
        this.enabled = enabled;
        this.signingKey = requireSigningKey(signingKey, enabled);
        this.rawTokenBytes = requireMin(rawTokenBytes, MIN_RAW_TOKEN_BYTES, "raw-token-bytes");
    }

    private static String requireSigningKey(String value, boolean enabled) {
        if (!enabled) {
            return "";
        }

        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    "app.security.csrf.signing-key is required when CSRF is enabled");
        }

        return value.trim();
    }

    private static int requireMin(int value, int min, String propertyName) {
        if (value < min) {
            throw new IllegalArgumentException(
                    "app.security.csrf." + propertyName + " must be >= " + min);
        }

        return value;
    }

}
