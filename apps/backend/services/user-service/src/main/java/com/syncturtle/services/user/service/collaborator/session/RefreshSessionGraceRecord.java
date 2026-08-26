package com.syncturtle.services.user.service.collaborator.session;

import java.time.Instant;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class RefreshSessionGraceRecord {

    public static final int CURRENT_GRACE_VERSION = 1;

    private static final Pattern SHA_256_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private final int graceVersion;
    private final String previousRefreshTokenHash;
    private final String successorRefreshTokenHash;
    private final long successorRotationCounter;
    private final String successorEnvelope;
    private final String clientBindingHash;
    private final Instant expiresAt;
    private final long expiresAtEpochMilli;
    private final boolean consumed;

    @Builder
    @Jacksonized
    private RefreshSessionGraceRecord(
            int graceVersion,
            String previousRefreshTokenHash,
            String successorRefreshTokenHash,
            Long successorRotationCounter,
            String successorEnvelope,
            String clientBindingHash,
            Instant expiresAt,
            Long expiresAtEpochMilli,
            boolean consumed) {
        Assert.isTrue(graceVersion == CURRENT_GRACE_VERSION, "unsupported refresh session grace version");
        Assert.notNull(successorRotationCounter, "successorRotationCounter is required");
        Assert.isTrue(successorRotationCounter > 0, "successorRotationCounter must be positive");
        Assert.hasText(successorEnvelope, "successorEnvelope is required");
        Assert.notNull(expiresAt, "expiresAt is required");
        Assert.notNull(expiresAtEpochMilli, "expiresAtEpochMilli is required");

        this.graceVersion = graceVersion;
        this.previousRefreshTokenHash = validateAndNormalizeSha256(previousRefreshTokenHash,
                "previousRefreshTokenHash");
        this.successorRefreshTokenHash = validateAndNormalizeSha256(successorRefreshTokenHash,
                "successorRefreshTokenHash");
        this.successorRotationCounter = successorRotationCounter;
        this.successorEnvelope = successorEnvelope;
        this.clientBindingHash = validateAndNormalizeSha256(clientBindingHash, "clientBindingHash");
        this.expiresAt = expiresAt;
        this.expiresAtEpochMilli = expiresAtEpochMilli;
        this.consumed = consumed;

        Assert.isTrue(!this.previousRefreshTokenHash.equals(this.successorRefreshTokenHash),
                "previous and successor refresh token hashes must differ");
        Assert.isTrue(this.expiresAt.toEpochMilli() == this.expiresAtEpochMilli,
                "expiresAtEpochMilli must represent expiresAt exactly");
    }

    private static String validateAndNormalizeSha256(String value, String fieldName) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(SHA_256_PATTERN.matcher(normalized).matches(),
                fieldName + " must be a lowercase SHA-256 hex value");

        return normalized;
    }

}
