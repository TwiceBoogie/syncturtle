package com.syncturtle.common.contracts.user.event;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import com.syncturtle.common.contracts.messaging.OutboxEvent;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class UserAuthenticatedEvent implements OutboxEvent {

    public static final String EVENT_TYPE = "USER_AUTHENTICATED";

    private final String eventId;
    private final Instant occurredAt;

    private final UUID userId;
    private final String email;
    private final boolean createdUser;
    private final String provider;

    @Builder
    @Jacksonized
    private UserAuthenticatedEvent(
            String eventId,
            Instant occurredAt,
            UUID userId,
            String email,
            Boolean createdUser,
            String provider) {
        this.eventId = requireText(eventId, "eventId is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.userId = Objects.requireNonNull(userId, "userId is required");
        this.email = normalizeEmail(email);
        this.createdUser = requireBoolean(createdUser, "createdUser is required");
        this.provider = requireText(provider, "provider is required");
    }

    @Override
    public String eventTypeName() {
        return EVENT_TYPE;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static String normalizeEmail(String value) {
        String normalized = requireText(value, "email is required").toLowerCase(Locale.ROOT);

        if (!normalized.contains("@")) {
            throw new IllegalArgumentException("email is invalid");
        }

        return normalized;
    }

    private static boolean requireBoolean(Boolean value, String mesage) {
        if (value == null) {
            throw new IllegalArgumentException(mesage);
        }

        return value;
    }

}
