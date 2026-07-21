package com.syncturtle.common.contracts.instance.event;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class InstanceAdminSecurityEvent {

    public enum Type {
        ADMIN_GRANTED,
        ADMIN_ROLE_CHANGED,
        ADMIN_REVOKED,
        ADMIN_SESSION_REVOKED
    }

    private final String eventId;
    private final Instant occurredAt;
    private final Type type;
    private final UUID instanceId;
    private final UUID userId;
    // security state owned by the instance-service
    private final Long sessionVersion;
    private final boolean active;
    private final List<String> roles;

    @Builder
    @Jacksonized
    private InstanceAdminSecurityEvent(
            String eventId,
            Instant occurredAt,
            Type type,
            UUID instanceId,
            UUID userId,
            Long sessionVersion,
            boolean active,
            List<String> roles) {
        this.eventId = requireText(eventId, "eventId is required");
        this.occurredAt = requirePastOrPresent(occurredAt, "Event occurredAt timestamp cannot be in the future");
        this.type = Objects.requireNonNull(type, "type is required");
        this.instanceId = Objects.requireNonNull(instanceId, "instanceId is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.sessionVersion = requirePositive(sessionVersion, "sessionVersion must be greater than zero");
        this.active = active;
        this.roles = requireNotEmpty(roles, "admin roles list cannot be null or empty");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static Long requirePositive(Long value, String message) {
        if (value == null || value <= 0L) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static Instant requirePastOrPresent(Instant value, String message) {
        if (value == null || value.isAfter(Instant.now())) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static List<String> requireNotEmpty(List<String> list, String message) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException(message);
        }

        return list;
    }

}
