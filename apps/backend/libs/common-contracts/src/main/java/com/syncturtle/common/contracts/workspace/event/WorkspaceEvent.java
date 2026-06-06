package com.syncturtle.common.contracts.workspace.event;

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
public final class WorkspaceEvent {

    public enum Type {
        WORKSPACE_CREATED,
        WORKSPACE_UPDATED,
        WORKSPACE_SOFT_DELETE
    }

    private static final long INITIAL_VERSION = 0L;

    private final String eventId;
    private final Instant occurredAt;
    private final Type type;

    private final UUID id;
    private final String name;
    private final String logo;
    private final UUID logoAssetId;
    private final String slug;
    private final String organizationSize;
    private final UUID ownerId;
    private final String timezone;
    private final Long totalMembers;
    private final UUID createdById;
    private final UUID updatedById;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;
    private final Long version;

    private WorkspaceEvent(
            String eventId,
            Instant occurredAt,
            Type type,
            UUID id,
            String name,
            String logo,
            UUID logoAssetId,
            String slug,
            String organizationSize,
            UUID ownerId,
            String timezone,
            Long totalMembers,
            UUID createdById,
            UUID updatedById,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            Long version) {
        this.eventId = requireText(eventId, "eventId is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
        this.type = Objects.requireNonNull(type, "type is required");

        this.id = Objects.requireNonNull(id, "id is required");

        this.name = requireText(name, "name is required");
        this.logo = normalizeNullable(logo);
        this.logoAssetId = logoAssetId;
        this.slug = normalizeSlug(slug);
        this.organizationSize = normalizeNullable(organizationSize);
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId is required");
        this.timezone = requireText(timezone, "timezone is required");

        this.totalMembers = requireNonNegativeLong(totalMembers, "totalMembers is required");

        this.createdById = createdById;
        this.updatedById = updatedById;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.deletedAt = deletedAt;

        this.version = requireNonNegative(version, "version is required");

        requireDeleteEventShape();
    }

    public boolean isCreateEvent() {
        return type == Type.WORKSPACE_CREATED;
    }

    public boolean isUpdateEvent() {
        return type == Type.WORKSPACE_UPDATED;
    }

    public boolean isDeleteEvent() {
        return type == Type.WORKSPACE_SOFT_DELETE;
    }

    public boolean isTombstoneEvent() {
        return isDeleteEvent();
    }

    private void requireDeleteEventShape() {
        if (!isDeleteEvent()) {
            return;
        }

        if (deletedAt == null) {
            throw new IllegalArgumentException("soft-delete workspace event must have deletedAt");
        }
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

    private static Long requireNonNegativeLong(Long value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        if (value < 0) {
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

    private static String normalizeSlug(String value) {
        return requireText(value, "slug is required").toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

}
