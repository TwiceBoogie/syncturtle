package com.syncturtle.common.contracts.user.event;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Jacksonized
@Builder(toBuilder = true)
public final class UserEvent {

    public enum Type {
        USER_CREATED, USER_UPDATED, USER_SOFT_DELETE
    }

    private static final long INITIAL_VERSION = 0L;

    private final String eventId;
    private final Instant occurredAt;
    private final Type type;

    private final UUID id;
    private final String username;
    private final String email;
    private final String displayName;
    private final String firstName;
    private final String lastName;
    private final Instant dateJoined;
    private final UUID avatarAssetId;
    private final UUID coverImageAssetId;
    private final boolean active;
    private final boolean emailVerified;
    private final boolean passwordAutoset;
    private final String userTimezone;
    private final boolean bot;
    private final Long version;
    private final Long authVersion;

    private UserEvent(
            String eventId,
            Instant occurredAt,
            Type type,
            UUID id,
            String username,
            String email,
            String displayName,
            String firstName,
            String lastName,
            Instant dateJoined,
            UUID avatarAssetId,
            UUID coverImageAssetId,
            Boolean active,
            Boolean emailVerified,
            Boolean passwordAutoset,
            String userTimezone,
            Boolean bot,
            Long version,
            Long authVersion) {
        this.eventId = requireText(eventId, "eventId is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.type = Objects.requireNonNull(type, "type is required");

        this.id = Objects.requireNonNull(id, "id is required");
        this.username = requireText(username, "username is required");
        this.email = normalizeEmail(email);
        this.displayName = requireText(displayName, "displayName is required");
        this.firstName = normalizeNullable(firstName);
        this.lastName = normalizeNullable(lastName);
        this.dateJoined = Objects.requireNonNull(dateJoined, "dateJoined is required");

        this.avatarAssetId = avatarAssetId;
        this.coverImageAssetId = coverImageAssetId;

        this.active = requireBoolean(active, "active is required");
        this.emailVerified = requireBoolean(emailVerified, "emailVerified is required");
        this.passwordAutoset = requireBoolean(passwordAutoset, "passwordAutoset is required");
        this.userTimezone = requireText(userTimezone, "userTimezone is required");
        this.bot = requireBoolean(bot, "bot is required");

        this.version = requireNonNegative(version, "version is required");
        this.authVersion = requireNonNegative(authVersion, "authVersion is required");

        requireDeleteEventShape();
    }

    public boolean isDeleteEvent() {
        return type == Type.USER_SOFT_DELETE;
    }

    public boolean isCreateEvent() {
        return type == Type.USER_CREATED;
    }

    public boolean isUpdateEvent() {
        return type == Type.USER_UPDATED;
    }

    public boolean isTombstoneEvent() {
        return isDeleteEvent();
    }

    private void requireDeleteEventShape() {
        if (!isDeleteEvent()) {
            return;
        }

        if (active) {
            throw new IllegalArgumentException("soft-delete user event must have active=false");
        }
    }

    private static boolean requireBoolean(Boolean value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static Long requireNonNegative(Long value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        if (value < INITIAL_VERSION) {
            throw new IllegalArgumentException(message + " and must be greater than or equal to 0");
        }

        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
