package com.syncturtle.services.user.configuration.property;

import java.util.Base64;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.auth.refresh-session.client-binding", ignoreUnknownFields = false)
public final class RefreshSessionClientBindingProperties {

    public static final int KEY_BYTES = 32;

    private final byte[] hmacKey;

    public RefreshSessionClientBindingProperties(String hmacKey) {
        if (!StringUtils.hasText(hmacKey)) {
            throw new IllegalArgumentException(
                    "app.auth.refresh-session.client-binding.hmac-key is required");
        }

        byte[] decoded;
        try {
            decoded = Base64.getUrlDecoder().decode(hmacKey.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "app.auth.refresh-session.client-binding.hmac-key must be base64url", exception);
        }

        if (decoded.length != KEY_BYTES) {
            throw new IllegalArgumentException(
                    "app.auth.refresh-session.client-binding.hmac-key must decode to 32 bytes");
        }

        this.hmacKey = decoded.clone();
    }

    public byte[] getHmacKey() {
        return hmacKey.clone();
    }
}
