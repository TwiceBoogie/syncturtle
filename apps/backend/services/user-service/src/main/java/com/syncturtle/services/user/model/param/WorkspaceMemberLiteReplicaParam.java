package com.syncturtle.services.user.model.param;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class WorkspaceMemberLiteReplicaParam {

    private final UUID id;
    private final UUID workspaceId;
    private final UUID memberId;
    private final int role;
    private final boolean active;
    private final UUID createdById;
    private final UUID updatedById;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;
    private final Long sourceVersion;

    @Builder
    private WorkspaceMemberLiteReplicaParam(
            UUID id,
            UUID workspaceId,
            UUID memberId,
            int role,
            Boolean active,
            UUID createdById,
            UUID updatedById,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            Long sourceVersion) {
        Assert.notNull(id, "id is required");
        Assert.notNull(workspaceId, "workspaceId is required");
        Assert.notNull(memberId, "memberId is required");
        Assert.isTrue(role >= 0, "role must be greater or equal to zero");
        Assert.notNull(createdAt, "createdAt is required");
        Assert.notNull(updatedAt, "updatedAt is required");
        Assert.notNull(sourceVersion, "sourceVersion is required");

        this.id = id;
        this.workspaceId = workspaceId;
        this.memberId = memberId;
        this.role = role;
        this.active = active;
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
