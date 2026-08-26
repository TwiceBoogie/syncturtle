package com.syncturtle.common.contracts.auth.session;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class RefreshSessionFamilyRecord {

    public static final int CURRENT_RECORD_VERSION = 2;
    public static final Duration IDLE_LIFETIME = Duration.ofDays(7);
    public static final Duration ABSOLUTE_LIFETIME = Duration.ofDays(30);

    private static final String INSTANCE_ADMIN_ROLE = "INSTANCE_ADMIN";
    private static final int MAX_DEVICE_LABEL_LENGTH = 120;
    private static final Pattern ROLE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]*");
    private static final Pattern SHA_256_PATTERN = Pattern.compile("[0-9a-f]{64}");

    private final int recordVersion;
    private final String userId;
    private final String instanceId;
    private final List<String> roles;
    private final long authVersion;
    private final Long adminSessionVersion;
    private final String currentRefreshTokenHash;
    private final long rotationCounter;
    private final Instant createdAt;
    private final Instant lastUsedAt;
    private final Instant idleExpiresAt;
    private final Instant absoluteExpiresAt;
    private final String deviceLabel;
    private final String clientBindingHash;

    @Builder
    @Jacksonized
    private RefreshSessionFamilyRecord(
            int recordVersion,
            String userId,
            String instanceId,
            List<String> roles,
            Long authVersion,
            Long adminSessionVersion,
            String currentRefreshTokenHash,
            Long rotationCounter,
            Instant createdAt,
            Instant lastUsedAt,
            Instant idleExpiresAt,
            Instant absoluteExpiresAt,
            String deviceLabel,
            String clientBindingHash) {
        if (recordVersion != CURRENT_RECORD_VERSION) {
            throw new IllegalArgumentException("unsupported refresh session family record version");
        }

        this.recordVersion = recordVersion;
        this.userId = requireUuid(userId, "userId");
        this.instanceId = requireUuid(instanceId, "instanceId");
        this.roles = requireCanonicalRoles(roles);
        this.authVersion = requireVersion(authVersion, "authVersion");
        this.adminSessionVersion = optionalVersion(adminSessionVersion, "adminSessionVersion");
        this.currentRefreshTokenHash = requireSha256(currentRefreshTokenHash, "currentRefreshTokenHash");
        this.rotationCounter = requireRotationCounter(rotationCounter);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.lastUsedAt = Objects.requireNonNull(lastUsedAt, "lastUsedAt is required");
        this.idleExpiresAt = Objects.requireNonNull(idleExpiresAt, "idleExpiresAt is required");
        this.absoluteExpiresAt = Objects.requireNonNull(absoluteExpiresAt, "absoluteExpiresAt is required");
        this.deviceLabel = optionalText(deviceLabel, "deviceLabel", MAX_DEVICE_LABEL_LENGTH);
        this.clientBindingHash = requireSha256(clientBindingHash, "clientBindingHash");

        validateAdminClassification();
        validateTimestamps();
    }

    public long nextRotationCounter() {
        return rotationCounter + 1;
    }

    private void validateAdminClassification() {
        boolean hasAdminRole = roles.contains(INSTANCE_ADMIN_ROLE);
        boolean hasAdminVersion = adminSessionVersion != null;

        if (hasAdminRole != hasAdminVersion) {
            throw new IllegalArgumentException(
                    "INSTANCE_ADMIN role and adminSessionVersion must either both be present or both be absent");
        }
    }

    private void validateTimestamps() {
        if (lastUsedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("lastUsedAt must not be before createdAt");
        }
        if (!absoluteExpiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException("absoluteExpiresAt must be after createdAt");
        }
        if (!idleExpiresAt.isAfter(lastUsedAt)) {
            throw new IllegalArgumentException("idleExpiresAt must be after lastUsedAt");
        }
        if (idleExpiresAt.isAfter(absoluteExpiresAt)) {
            throw new IllegalArgumentException("idleExpiresAt must not be after absoluteExpiresAt");
        }

        Instant requiredAbsoluteExpiresAt = createdAt.plus(ABSOLUTE_LIFETIME);
        if (!absoluteExpiresAt.equals(requiredAbsoluteExpiresAt)) {
            throw new IllegalArgumentException("absoluteExpiresAt must be exactly 30 days after createdAt");
        }

        Instant requiredIdleExpiresAt = lastUsedAt.plus(IDLE_LIFETIME);
        if (requiredIdleExpiresAt.isAfter(absoluteExpiresAt)) {
            requiredIdleExpiresAt = absoluteExpiresAt;
        }
        if (!idleExpiresAt.equals(requiredIdleExpiresAt)) {
            throw new IllegalArgumentException(
                    "idleExpiresAt must equal the earlier of 7 days after lastUsedAt and absoluteExpiresAt");
        }
    }

    private static String requireUuid(String value, String fieldName) {
        String normalized = requireText(value, fieldName + " is required");
        UUID parsed = UUID.fromString(normalized);
        String canonical = parsed.toString().toLowerCase(Locale.ROOT);

        if (!canonical.equals(normalized.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(fieldName + " must be canonical UUID");
        }
        return canonical;
    }

    private static List<String> requireCanonicalRoles(List<String> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("at least 1 role is required");
        }

        List<String> normalized = new ArrayList<>(values.size());
        String previous = null;

        for (String value : values) {
            String role = requireText(value, "role is required");
            if (!ROLE_PATTERN.matcher(role).matches()) {
                throw new IllegalArgumentException("role must use canonical upper-snake-case");
            }
            if (previous != null && previous.compareTo(role) >= 0) {
                throw new IllegalArgumentException("roles must be unique and sorted");
            }
            normalized.add(role);
            previous = role;
        }

        return Collections.unmodifiableList(normalized);
    }

    private static long requireVersion(Long value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value;
    }

    private static Long optionalVersion(Long value, String fieldName) {
        if (value == null) {
            return null;
        }
        return requireVersion(value, fieldName);
    }

    private static long requireRotationCounter(Long value) {
        if (value == null) {
            throw new IllegalArgumentException("rotationCounter is required");
        }
        if (value < 0 || value == Long.MAX_VALUE) {
            throw new IllegalArgumentException("rotationCounter must be between 0 and Long.MAX_VALUE - 1");
        }
        return value;
    }

    private static String requireSha256(String value, String fieldName) {
        String normalized = requireText(value, fieldName + " is required");
        if (!SHA_256_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName + " must be a lowercase SHA-256 hex value");
        }
        return normalized;
    }

    private static String optionalText(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        return normalized;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

}
