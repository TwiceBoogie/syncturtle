package com.syncturtle.services.user.model.param;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class WorkspaceLiteReplicaParam {

    private final UUID id;
    private final String name;
    private final UUID logoAssetId;
    private final String slug;
    private final UUID createdById;
    private final UUID updatedById;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant deletedAt;
    private final Long sourceVersion;

    @Builder
    private WorkspaceLiteReplicaParam(
            UUID id,
            String name,
            UUID logoAssetId,
            String slug,
            UUID createdById,
            UUID updatedById,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            Long sourceVersion) {
        Assert.notNull(id, "id is required");
        Assert.hasText(name, "name is required");
        Assert.hasText(slug, "slug is required");
        Assert.notNull(createdAt, "createdAt is required");
        Assert.notNull(updatedAt, "updatedAt is required");
        Assert.notNull(sourceVersion, "sourceVersion is required");

        this.id = id;
        this.name = name.trim();
        this.logoAssetId = logoAssetId;
        this.slug = slug.trim().toLowerCase();
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
