package com.syncturtle.common.contracts.auth.session;

import java.util.Objects;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class CreateInstanceAdminSessionHandoffRequest {

    private final UUID userId;
    private final UUID instanceId;
    private final long userAuthVersion;
    private final long adminSessionVersion;
    private final PreAuthTransactionBinding preAuthBinding;
    private final String clientIp;
    private final String userAgent;

    @Builder
    @Jacksonized
    private CreateInstanceAdminSessionHandoffRequest(
            UUID userId,
            UUID instanceId,
            Long userAuthVersion,
            Long adminSessionVersion,
            PreAuthTransactionBinding preAuthBinding,
            String clientIp,
            String userAgent) {
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId is required");
        this.userAuthVersion = requireVersion(userAuthVersion, "userAuthVersion");
        this.adminSessionVersion = requireVersion(adminSessionVersion, "adminSessionVersion");
        this.preAuthBinding = Objects.requireNonNull(preAuthBinding, "preAuthBinding is required");
        this.clientIp = normalizeNullable(clientIp);
        this.userAgent = normalizeNullable(userAgent);
    }

    private static long requireVersion(Long value, String name) {
        Objects.requireNonNull(value, name + " is required");

        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        return value;
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
