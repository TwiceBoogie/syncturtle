package com.syncturtle.services.user.model;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.services.user.model.param.WorkspaceMemberInviteLiteReplicaParam;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "workspace_member_invites_lite")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMemberInviteLite {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "accepted", nullable = false)
    private boolean accepted;

    @Column(name = "role", nullable = false)
    private int role;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "source_version", nullable = false)
    private Long sourceVersion;

    @Column(name = "created_by_id")
    private UUID createdById;

    @Column(name = "updated_by_id")
    private UUID updatedById;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "projected_at", nullable = false)
    private Instant projectedAt;

    public static WorkspaceMemberInviteLite fromReplicaParam(WorkspaceMemberInviteLiteReplicaParam param, Clock clock) {
        Assert.notNull(param, "workspace member invite lite replica is required");
        Assert.notNull(clock, "clock is required");

        WorkspaceMemberInviteLite invite = new WorkspaceMemberInviteLite();
        invite.applyReplicaParam(param, clock);
        return invite;
    }

    public boolean applyReplicaParam(WorkspaceMemberInviteLiteReplicaParam param, Clock clock) {
        Assert.notNull(param, "workspace member invite lite replica is required");
        Assert.notNull(clock, "clock is required");

        requireSameInviteOrUninitialized(param);

        if (param.isStaleComparedTo(sourceVersion)) {
            return false;
        }

        boolean changed = false;

        changed |= setIdIfMissing(param.getId());
        changed |= setWorkspaceId(param.getWorkspaceId());
        changed |= setEmail(param.getEmail());
        changed |= setAccepted(param.isAccepted());
        changed |= setRole(param.getRole());
        changed |= setRespondedAt(param.getRespondedAt());
        changed |= setSourceVersion(param.getSourceVersion());
        changed |= setCreatedById(param.getCreatedById());
        changed |= setUpdatedById(param.getUpdatedById());
        changed |= setCreatedAt(param.getCreatedAt());
        changed |= setUpdatedAt(param.getUpdatedAt());
        changed |= setDeletedAt(param.getDeletedAt());

        projectedAt = Instant.now(clock);

        return changed;
    }

    private void requireSameInviteOrUninitialized(WorkspaceMemberInviteLiteReplicaParam param) {
        if (id == null) {
            return;
        }

        Assert.isTrue(id.equals(param.getId()), "cannot apply replica data for a different workspace member invite");
    }

    private boolean setIdIfMissing(UUID id) {
        if (this.id != null) {
            return false;
        }

        this.id = id;
        return true;
    }

    private boolean setWorkspaceId(UUID workspaceId) {
        if (Objects.equals(this.workspaceId, workspaceId)) {
            return false;
        }

        this.workspaceId = workspaceId;
        return true;
    }

    private boolean setEmail(String email) {
        String normalized = normalizeEmail(email);
        if (Objects.equals(this.email, normalized)) {
            return false;
        }

        this.email = normalized;
        return true;
    }

    private boolean setAccepted(boolean accepted) {
        if (this.accepted == accepted) {
            return false;
        }

        this.accepted = accepted;
        return true;
    }

    private boolean setRole(int role) {
        if (this.role == role) {
            return false;
        }

        this.role = role;
        return true;
    }

    private boolean setRespondedAt(Instant respondedAt) {
        if (Objects.equals(this.respondedAt, respondedAt)) {
            return false;
        }

        this.respondedAt = respondedAt;
        return true;
    }

    private boolean setSourceVersion(Long sourceVersion) {
        if (Objects.equals(this.sourceVersion, sourceVersion)) {
            return false;
        }

        this.sourceVersion = sourceVersion;
        return true;
    }

    private boolean setCreatedById(UUID createdById) {
        if (Objects.equals(this.createdById, createdById)) {
            return false;
        }

        this.createdById = createdById;
        return true;
    }

    private boolean setUpdatedById(UUID updatedById) {
        if (Objects.equals(this.updatedById, updatedById)) {
            return false;
        }

        this.updatedById = updatedById;
        return true;
    }

    private boolean setCreatedAt(Instant createdAt) {
        if (Objects.equals(this.createdAt, createdAt)) {
            return false;
        }

        this.createdAt = createdAt;
        return true;
    }

    private boolean setUpdatedAt(Instant updatedAt) {
        if (Objects.equals(this.updatedAt, updatedAt)) {
            return false;
        }

        this.updatedAt = updatedAt;
        return true;
    }

    private boolean setDeletedAt(Instant deletedAt) {
        if (Objects.equals(this.deletedAt, deletedAt)) {
            return false;
        }

        this.deletedAt = deletedAt;
        return true;
    }

    private static String normalizeEmail(String email) {
        Assert.hasText(email, "email is required");

        return email.trim().toLowerCase(Locale.ROOT);
    }

}
