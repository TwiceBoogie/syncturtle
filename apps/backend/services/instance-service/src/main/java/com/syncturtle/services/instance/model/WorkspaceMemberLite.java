package com.syncturtle.services.instance.model;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.services.instance.model.param.WorkspaceMemberLiteReplicaParam;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "workspace_members_lite")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceMemberLite {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private WorkspaceLite workspace;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "role", nullable = false)
    private int role;

    @Column(name = "is_active", nullable = false)
    private boolean active;

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

    public static WorkspaceMemberLite fromReplicaParam(WorkspaceMemberLiteReplicaParam param, Clock clock) {
        Assert.notNull(param, "workspace member lite replica param is required");
        Assert.notNull(clock, "clock is required");

        WorkspaceMemberLite member = new WorkspaceMemberLite();
        member.applyReplicaParam(param, clock);
        return member;
    }

    public boolean applyReplicaParam(WorkspaceMemberLiteReplicaParam param, Clock clock) {
        Assert.notNull(param, "workspace member lite replica param is required");
        Assert.notNull(clock, "clock is required");

        requireSameMemberOrUninitialized(param);

        if (param.isStaleComparedTo(sourceVersion)) {
            return false;
        }

        boolean changed = false;

        changed |= setIdIfMissing(param.getId());
        changed |= setWorkspaceId(param.getWorkspaceId());
        changed |= setMemberId(param.getMemberId());
        changed |= setRole(param.getRole());
        changed |= setActive(param.isActive());
        changed |= setCreatedById(param.getCreatedById());
        changed |= setUpdatedById(param.getUpdatedById());
        changed |= setCreatedAt(param.getCreatedAt());
        changed |= setUpdatedAt(param.getUpdatedAt());
        changed |= setDeletedAt(param.getDeletedAt());
        changed |= setSourceVersion(param.getSourceVersion());

        projectedAt = Instant.now(clock);

        return changed;
    }

    private void requireSameMemberOrUninitialized(WorkspaceMemberLiteReplicaParam param) {
        if (id == null) {
            return;
        }

        Assert.isTrue(id.equals(param.getId()), "cannot apply replica data for a different workspace member");
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

    private boolean setMemberId(UUID memberId) {
        if (Objects.equals(this.memberId, memberId)) {
            return false;
        }

        this.memberId = memberId;
        return true;
    }

    private boolean setRole(int role) {
        if (this.role == role) {
            return false;
        }

        this.role = role;
        return true;
    }

    private boolean setActive(boolean active) {
        if (this.active == active) {
            return false;
        }

        this.active = active;
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

}
