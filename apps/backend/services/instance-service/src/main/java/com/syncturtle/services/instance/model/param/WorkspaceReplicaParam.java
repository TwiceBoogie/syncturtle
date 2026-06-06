package com.syncturtle.services.instance.model.param;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.Assert;

import lombok.Getter;

@Getter
public final class WorkspaceReplicaParam {

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
    private final Long sourceVersion;

    private WorkspaceReplicaParam(
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
            Long sourceVersion) {
        Assert.notNull(id, "id is required");
        Assert.hasText(name, "name is required");
        Assert.hasText(slug, "slug is required");
        Assert.notNull(ownerId, "ownerId is required");
        Assert.hasText(timezone, "timezone is required");
        Assert.isTrue(totalMembers >= 0, "totalMembers must be greater than or equal to 0");
        Assert.notNull(createdAt, "createdAt is required");
        Assert.notNull(updatedAt, "updatedAt is required");
        Assert.notNull(sourceVersion, "sourceVersion is required");

        this.id = id;
        this.name = name.trim();
        this.logo = normalizeNullable(logo);
        this.logoAssetId = logoAssetId;
        this.slug = slug.trim().toLowerCase();
        this.organizationSize = normalizeNullable(organizationSize);
        this.ownerId = ownerId;
        this.timezone = timezone.trim();
        this.totalMembers = totalMembers;
        this.createdById = createdById;
        this.updatedById = updatedById;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.sourceVersion = sourceVersion;
    }

    public static WorkspaceReplicaParam of(
            UUID id,
            String name,
            String logo,
            UUID logoAssetId,
            String slug,
            String organizationSize,
            UUID ownerId,
            String timezone,
            long totalMembers,
            UUID createdById,
            UUID updatedById,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            Long sourceVersion) {
        return new WorkspaceReplicaParam(
                id,
                name,
                logo,
                logoAssetId,
                slug,
                organizationSize,
                ownerId,
                timezone,
                totalMembers,
                createdById,
                updatedById,
                createdAt,
                updatedAt,
                deletedAt,
                sourceVersion);
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

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isEmpty() ? null : normalized;
    }

}
