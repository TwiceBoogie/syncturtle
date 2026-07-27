package com.syncturtle.common.contracts.instance.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.syncturtle.common.contracts.instance.model.InstanceEdition;
import com.syncturtle.common.contracts.messaging.OutboxEvent;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class InstanceEvent implements OutboxEvent {
    public enum Type {
        INSTANCE_CREATED, INSTANCE_UPDATED, INSTANCE_SOFT_DELETED
    }

    private final String eventId;
    private final Instant occurredAt;
    private final Type type;
    private final UUID id;
    private final InstanceEdition edition;
    private final boolean setupDone;
    private final Long version;
    private final String apiBaseUrl;
    private final boolean test;
    private final Instant updatedAt;
    private final Instant createdAt;

    @Builder
    @Jacksonized
    private InstanceEvent(
            String eventId,
            Instant occurredAt,
            Type type,
            UUID id,
            InstanceEdition edition,
            boolean setupDone,
            Long version,
            String apiBaseUrl,
            boolean test,
            Instant updatedAt,
            Instant createdAt) {
        this.eventId = requireText(eventId, "eventId is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.type = Objects.requireNonNull(type, "type is required");
        this.id = Objects.requireNonNull(id, "id is required");
        this.edition = Objects.requireNonNull(edition, "edition is required");
        this.setupDone = setupDone;
        this.version = Objects.requireNonNull(version, "version is required");
        this.apiBaseUrl = normalizeNullable(apiBaseUrl);
        this.test = test;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
    }

    @Override
    public String eventTypeName() {
        return type.name();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
