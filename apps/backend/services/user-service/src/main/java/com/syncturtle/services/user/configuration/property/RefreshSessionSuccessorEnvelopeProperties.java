package com.syncturtle.services.user.configuration.property;

import java.util.Base64;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.auth.refresh-session.successor-envelope", ignoreUnknownFields = false)
public final class RefreshSessionSuccessorEnvelopeProperties {

    public static final int KEY_BYTES = 32;

    private final byte[] encryptionKey;

    public RefreshSessionSuccessorEnvelopeProperties(String encryptionKey) {
        if (!StringUtils.hasText(encryptionKey)) {
            throw new IllegalArgumentException(
                    "app.auth.refresh-session.successor-envelope.encryption-key is required");
        }

        byte[] decoded;
        try {
            decoded = Base64.getUrlDecoder().decode(encryptionKey.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "app.auth.refresh-session.successor-envelope.encryption-key must be base64url", exception);
        }

        if (decoded.length != KEY_BYTES) {
            throw new IllegalArgumentException(
                    "app.auth.refresh-session.successor-envelope.encryption-key must decode to 32 bytes");
        }

        this.encryptionKey = decoded.clone();
    }

    public byte[] getEncryptionKey() {
        return encryptionKey.clone();
    }

}
