package com.syncturtle.services.instance.model;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.Assert;

import com.syncturtle.services.instance.model.param.WorkspaceReplicaParam;

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
@Table(name = "workspaces_lite")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkspaceLite {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "logo_asset_id")
    private UUID logoAssetId;

    @Column(name = "slug", nullable = false, length = 48)
    private String slug;

    @Column(name = "organization_size", length = 20)
    private String organizationSize;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private UserLite owner;

    @Column(name = "timezone", nullable = false, length = 255)
    private String timezone;

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

    public static WorkspaceLite fromReplicaParam(WorkspaceReplicaParam param, Clock clock) {
        Assert.notNull(param, "workspace replica param is required");
        Assert.notNull(clock, "clock is required");

        WorkspaceLite workspace = new WorkspaceLite();
        workspace.applyReplicaParam(param, clock);
        return workspace;
    }

    public boolean applyReplicaParam(WorkspaceReplicaParam param, Clock clock) {
        Assert.notNull(param, "workspace replica param is required");
        Assert.notNull(clock, "clock is required");

        requireSameWorkspaceOrUninitialized(param);

        if (param.isStaleComparedTo(sourceVersion)) {
            return false;
        }

        boolean changed = false;

        changed |= setIdIfMissing(param.getId());
        changed |= setSourceVersion(param.getSourceVersion());
        changed |= setName(param.getName());
        changed |= setLogoAssetId(param.getLogoAssetId());
        changed |= setSlug(param.getSlug());
        changed |= setOrganizationSize(param.getOrganizationSize());
        changed |= setOwnerId(param.getOwnerId());
        changed |= setTimezone(param.getTimezone());
        changed |= setCreatedById(param.getCreatedById());
        changed |= setUpdatedById(param.getUpdatedById());
        changed |= setCreatedAt(param.getCreatedAt());
        changed |= setUpdatedAt(param.getUpdatedAt());
        changed |= setDeletedAt(param.getDeletedAt());

        projectedAt = Instant.now(clock);

        return changed;
    }

    public boolean isDeletedReplica() {
        return deletedAt != null;
    }

    private void requireSameWorkspaceOrUninitialized(WorkspaceReplicaParam param) {
        if (id == null) {
            return;
        }

        Assert.isTrue(id.equals(param.getId()), "cannot apply a workspace replica param for a different workspace");
    }

    private boolean setIdIfMissing(UUID id) {
        if (this.id != null) {
            return false;
        }

        this.id = id;
        return true;
    }

    private boolean setSourceVersion(Long sourceVersion) {
        if (Objects.equals(this.sourceVersion, sourceVersion)) {
            return false;
        }

        this.sourceVersion = sourceVersion;
        return true;
    }

    private boolean setName(String name) {
        if (Objects.equals(this.name, name)) {
            return false;
        }

        this.name = name;
        return true;
    }

    private boolean setLogoAssetId(UUID logoAssetId) {
        if (Objects.equals(this.logoAssetId, logoAssetId)) {
            return false;
        }

        this.logoAssetId = logoAssetId;
        return true;
    }

    private boolean setSlug(String slug) {
        if (Objects.equals(this.slug, slug)) {
            return false;
        }

        this.slug = slug;
        return true;
    }

    private boolean setOrganizationSize(String organizationSize) {
        if (Objects.equals(this.organizationSize, organizationSize)) {
            return false;
        }

        this.organizationSize = organizationSize;
        return true;
    }

    private boolean setOwnerId(UUID ownerId) {
        if (Objects.equals(this.ownerId, ownerId)) {
            return false;
        }

        this.ownerId = ownerId;
        return true;
    }

    private boolean setTimezone(String timezone) {
        if (Objects.equals(this.timezone, timezone)) {
            return false;
        }

        this.timezone = timezone;
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