package com.syncturtle.services.file.model;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.data.jpa.entity.AuditedEntity;
import com.syncturtle.services.file.model.param.FileAssetLinkCreateParam;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "file_asset_links")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileAssetLink extends AuditedEntity {

    @Column(name = "asset_id", nullable = false)
    private UUID assetId;

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "target_service", nullable = false, length = 64)
    private String targetService;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private UUID targetId;

    @Column(name = "usage_type", nullable = false, length = 64)
    private String usageType;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "linked_by_user_id")
    private UUID linkedByUserId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    private String attributes;

    @Column(name = "source_version", nullable = false)
    private Long sourceVersion;

    public static FileAssetLink create(FileAssetLinkCreateParam param) {
        Assert.notNull(param, "file asset link create param is required");

        FileAssetLink link = new FileAssetLink();
        link.initializeForCreate(param, true);
        return link;
    }

    public static FileAssetLink createDeletedPrimarySlot(FileAssetLinkCreateParam param, Clock clock) {
        Assert.notNull(param, "file asset link create param is required");
        Assert.notNull(clock, "clock is required");

        FileAssetLink link = new FileAssetLink();
        link.initializeForCreate(param, false);
        link.primary = true;
        link.assetId = null;
        link.softDelete(clock);

        return link;
    }

    public boolean pointsTo(UUID assetId) {
        return Objects.equals(this.assetId, assetId);
    }

    public boolean hasProcessedSourceVersion(Long sourceVersion) {
        Assert.notNull(sourceVersion, "sourceVersion is required");

        return this.sourceVersion != null && sourceVersion <= this.sourceVersion;
    }

    public boolean replaceAssetIfNewer(UUID assetId, UUID linkedByUserId, Long sourceVersion) {
        Assert.notNull(assetId, "assetId is required");
        Assert.notNull(sourceVersion, "sourceVersion is required");

        if (hasProcessedSourceVersion(sourceVersion)) {
            return false;
        }

        this.assetId = assetId;
        this.linkedByUserId = linkedByUserId;
        this.sourceVersion = sourceVersion;

        restore();

        return true;
    }

    public boolean markDeletedIfNewer(Long sourceVersion, Clock clock) {
        Assert.notNull(sourceVersion, "sourceVersion is required");
        Assert.notNull(clock, "clock is required");

        if (hasProcessedSourceVersion(sourceVersion)) {
            return false;
        }

        this.assetId = null;
        this.sourceVersion = sourceVersion;

        softDelete(clock);

        return true;
    }

    public boolean updateAttributes(String attributes) {
        String normalized = normalizeJsonOrDefault(attributes);

        if (Objects.equals(this.attributes, normalized)) {
            return false;
        }

        this.attributes = normalized;
        return true;
    }

    private void initializeForCreate(FileAssetLinkCreateParam param, boolean requireAsset) {
        Assert.notNull(param, "file asset link create param is required");

        if (requireAsset) {
            Assert.notNull(param.getAssetId(), "assetId is required");
        }

        Assert.notNull(param.getTargetId(), "targetId is required");
        Assert.notNull(param.getSourceVersion(), "sourceVersion is required");

        assetId = param.getAssetId();
        workspaceId = param.getWorkspaceId();
        targetService = param.getTargetService();
        targetType = param.getTargetType();
        targetId = param.getTargetId();
        usageType = param.getUsageType();
        primary = param.isPrimary();
        sourceVersion = param.getSourceVersion();
        linkedByUserId = param.getLinkedByUserId();
        attributes = param.getAttributes();
    }

    private static String normalizeJsonOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return "{}";
        }

        return value.trim();
    }

}