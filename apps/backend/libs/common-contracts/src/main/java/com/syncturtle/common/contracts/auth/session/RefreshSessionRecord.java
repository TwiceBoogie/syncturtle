package com.syncturtle.common.contracts.auth.session;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@JsonPropertyOrder({ "recordVersion", "userId", "instanceId", "roles", "authVersion",
        "adminSessionVersion", "currentRefreshTokenHash", "createdAt", "idleExpiresAt", "absoluteExpiresAt" })
public final class RefreshSessionRecord {

    public static final int CURRENT_RECORD_VERSION = 1;
    public static final Duration IDLE_LIFETIME = Duration.ofDays(7);
    public static final Duration ABSOLUTE_LIFETIME = Duration.ofDays(30);

    private static final Pattern ROLE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final Pattern HASH_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private final int recordVersion;
    private final String userId;
    private final String instanceId;
    private final List<String> roles;
    private final long authVersion;
    private final Long adminSessionVersion;
    private final String currentRefreshTokenHash;
    private final Instant createdAt;
    private final Instant idleExpiresAt;
    private final Instant absoluteExpiresAt;

    @Builder
    @Jacksonized
    private RefreshSessionRecord(
            int recordVersion,
            String userId,
            String instanceId,
            List<String> roles,
            Long authVersion,
            Long adminSessionVersion,
            String currentRefreshTokenHash,
            Instant createdAt,
            Instant idleExpiresAt,
            Instant absoluteExpiresAt) {
        if (recordVersion != CURRENT_RECORD_VERSION) {
            throw new IllegalArgumentException("unsupported session record version");
        }

        this.recordVersion = recordVersion;
        this.userId = canonicalUuid(userId, "userId");
        this.instanceId = canonicalUuid(instanceId, "instanceId");
        this.roles = canonicalRoles(roles);
        this.authVersion = requireVersion(authVersion, "authVersion");
        this.adminSessionVersion = adminSessionVersion == null ? null
                : requireVersion(adminSessionVersion, "adminSessionVersion");

        if (this.roles.contains("INSTANCE_ADMIN") != (this.adminSessionVersion != null)) {
            throw new IllegalArgumentException("administrator role and version must agree");
        }

        if (currentRefreshTokenHash == null || !HASH_PATTERN.matcher(currentRefreshTokenHash).matches()) {
            throw new IllegalArgumentException("currentRefreshTokenHash must be a lowercase SHA-256 hash");
        }

        this.currentRefreshTokenHash = currentRefreshTokenHash;
        this.createdAt = requireInstant(createdAt, "createdAt");
        this.idleExpiresAt = requireInstant(idleExpiresAt, "idleExpiresAt");
        this.absoluteExpiresAt = requireInstant(absoluteExpiresAt, "absoluteExpiresAt");

        if (!this.absoluteExpiresAt.equals(this.createdAt.plus(ABSOLUTE_LIFETIME))) {
            throw new IllegalArgumentException("absolute expiration must be fixed at creation");
        }

        if (!this.idleExpiresAt.isAfter(this.createdAt)
                || this.idleExpiresAt.isAfter(this.absoluteExpiresAt)) {
            throw new IllegalArgumentException("idle expiration is outside the session lifetime");
        }
    }

    private static String canonicalUuid(String value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }

        String canonical = UUID.fromString(value).toString().toLowerCase(Locale.ROOT);
        if (!canonical.equals(value)) {
            throw new IllegalArgumentException(name + " must be a canonical UUID");
        }

        return canonical;
    }

    private static List<String> canonicalRoles(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("roles are required");
        }

        List<String> copy = new ArrayList<>(values.size());
        String prior = null;

        for (String value : values) {
            if (value == null
                    || !ROLE_PATTERN.matcher(value).matches()
                    || (prior != null && prior.compareTo(value) >= 0)) {
                throw new IllegalArgumentException("roles must be sorted unique upper-snake-case values");
            }

            copy.add(value);
            prior = value;
        }
        return Collections.unmodifiableList(copy);
    }

    private static long requireVersion(Long value, String name) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException(name + " must be nonnegative");
        }

        return value;
    }

    private static Instant requireInstant(Instant value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required");
        }

        return value;
    }

}
