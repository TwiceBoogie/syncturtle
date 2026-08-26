package com.syncturtle.services.user.model.param;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class WorkspaceMemberInviteLiteReplicaParam {

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
    private final Long sourceVersion;

    @Builder
    private WorkspaceMemberInviteLiteReplicaParam(
            UUID id,
            UUID workspaceId,
            String email,
            Boolean accepted,
            int role,
            Instant respondedAt,
            UUID createdById,
            UUID updatedById,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            Long sourceVersion) {
        Assert.notNull(id, "id is required");
        Assert.notNull(workspaceId, "workspaceId is required");
        Assert.hasText(email, "email is required");
        Assert.notNull(accepted, "accepted is required");
        Assert.isTrue(role >= 0, "role must be greater than or equal to 0");
        Assert.notNull(createdAt, "createdAt is required");
        Assert.notNull(updatedAt, "updatedAt is required");
        Assert.notNull(sourceVersion, "sourceVersion is required");

        this.id = id;
        this.workspaceId = workspaceId;
        this.email = email.trim();
        this.accepted = accepted;
        this.role = role;
        this.respondedAt = respondedAt;
        this.createdById = createdById;
        this.updatedById = updatedById;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.sourceVersion = sourceVersion;
    }

    public boolean isStaleComparedTo(Long currentSourceVersion) {
        if (currentSourceVersion == null) {
            return false;
        }

        return sourceVersion < currentSourceVersion;
    }

    public boolean hasSameVersionAs(Long currentSourceVersion) {
        return Objects.equals(sourceVersion, currentSourceVersion);
    }

}
