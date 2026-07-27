package com.syncturtle.services.user.util;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
public final class PasswordResetUidCodec {

    private static final int UUID_BYTES = 16;
    private static final int ENCODED_UUID_LENGTH = 22;
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    public String encode(UUID userId) {
        Assert.notNull(userId, "userId is required");

        ByteBuffer buffer = ByteBuffer.allocate(UUID_BYTES);
        buffer.putLong(userId.getMostSignificantBits());
        buffer.putLong(userId.getLeastSignificantBits());

        return ENCODER.encodeToString(buffer.array());
    }

    public Optional<UUID> decode(String uidb64) {
        if (!StringUtils.hasText(uidb64)) {
            return Optional.empty();
        }

        String normalized = uidb64.trim();

        if (normalized.length() != ENCODED_UUID_LENGTH) {
            return Optional.empty();
        }

        try {
            byte[] decoded = DECODER.decode(normalized);

            if (decoded.length != UUID_BYTES) {
                return Optional.empty();
            }

            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            return Optional.of(new UUID(buffer.getLong(), buffer.getLong()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

}
