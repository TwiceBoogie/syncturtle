package com.syncturtle.common.contracts.auth.session;

import java.time.Instant;
import java.util.Objects;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class AdminSessionHandoffResponse {

    private final String completionCode;
    private final Instant issuedAt;
    private final Instant expiresAt;

    @Builder
    @Jacksonized
    private AdminSessionHandoffResponse(String completionCode, Instant issuedAt, Instant expiresAt) {
        this.completionCode = requireText(completionCode, "completionCode");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt is required");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

}
