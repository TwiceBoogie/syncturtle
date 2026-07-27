package com.syncturtle.common.contracts.workspace.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
public final class WorkspaceMemberInviteEvent implements WorkspaceOutboxEvent {

    public enum Type {
        WORKSPACE_MEMBER_INVITE_CREATED,
        WORKSPACE_MEMBER_INVITE_UPDATED,
        WORKSPACE_MEMBER_INVITE_SOFT_DELETE
    }

    private static final long INITIAL_VERSION = 0L;

    private final String eventId;
    private final Instant occurredAt;
    private final Type type;

    private final UUID id;
    private final UUID workspaceId;
    private final String email;
    private final boolean accepted;
    private final int role;
    private final Instant respondedAt;
    private final UUID createdById;
    private final UUID updatedById;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;
    private final Long version;

    @Builder
    @Jacksonized
    private WorkspaceMemberInviteEvent(
            String eventId,
            Instant occurredAt,
            Type type,
            UUID id,
            UUID workspaceId,
            String email,
            Boolean accepted,
            Integer role,
            Instant respondedAt,
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
        this.workspaceId = Objects.requireNonNull(workspaceId, "workspaceId is required");
        this.email = requireText(email, "email is required");
        this.accepted = requireBoolean(accepted, "accepted is required");
        this.role = role;
        this.respondedAt = respondedAt;
        this.createdById = createdById;
        this.updatedById = updatedById;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        this.deletedAt = deletedAt;

        this.version = requireNonNegative(version, "version is required");

        requireDeleteEventShape();
    }

    @Override
    public boolean isCreateEvent() {
        return type == Type.WORKSPACE_MEMBER_INVITE_CREATED;
    }

    @Override
    public boolean isUpdateEvent() {
        return type == Type.WORKSPACE_MEMBER_INVITE_UPDATED;
    }

    @Override
    public boolean isDeleteEvent() {
        return type == Type.WORKSPACE_MEMBER_INVITE_SOFT_DELETE;
    }

    @Override
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

    private static boolean requireBoolean(Boolean value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }

        return value;
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

}
