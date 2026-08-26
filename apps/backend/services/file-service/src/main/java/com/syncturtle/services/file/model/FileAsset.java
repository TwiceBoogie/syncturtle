package com.syncturtle.services.file.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.file.FileAssetPurpose;
import com.syncturtle.common.data.jpa.entity.AssignedIdAuditedEntity;
import com.syncturtle.services.file.model.param.FileAssetCreateParam;
import com.syncturtle.services.file.type.FileAssetStatus;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Access(AccessType.FIELD)
@Table(name = "file_assets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileAsset extends AssignedIdAuditedEntity {

    @Column(name = "workspace_id")
    private UUID workspaceId;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "storage_provider", nullable = false, length = 32)
    private String storageProvider;

    @Column(name = "bucket", nullable = false, length = 255)
    private String bucket;

    @Column(name = "object_key", nullable = false, length = 1000)
    private String objectKey;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(name = "extension", length = 20)
    private String extension;

    @Column(name = "declared_size_bytes", nullable = false)
    private long declaredSizeBytes;

    @Column(name = "actual_size_bytes")
    private Long actualSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 64)
    private FileAssetPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FileAssetStatus status;

    @Column(name = "upload_expires_at")
    private Instant uploadExpiresAt;

    @Column(name = "uploaded_at")
    private Instant uploadedAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deletedFlag;

    @Column(name = "storage_cleanup_token")
    private UUID storageCleanupToken;

    @Column(name = "storage_cleanup_claimed_until")
    private Instant storageCleanupClaimedUntil;

    @Column(name = "storage_deleted_at")
    private Instant storageDeletedAt;

    @Column(name = "storage_version_id", length = 255)
    private String storageVersionId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    private String attributes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "storage_metadata", nullable = false, columnDefinition = "jsonb")
    private String storageMetadata;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public static FileAsset create(FileAssetCreateParam param) {
        Assert.notNull(param, "file asset create param is required");

        FileAsset asset = new FileAsset();
        asset.initializeForCreate(param);
        return asset;
    }

    public boolean isUploaded() {
        return status == FileAssetStatus.UPLOADED && isActive();
    }

    public boolean isPendingUpload() {
        return status == FileAssetStatus.PENDING_UPLOAD && isActive();
    }

    public boolean isUploadExpired(Clock clock) {
        Assert.notNull(clock, "clock is required");

        return uploadExpiresAt != null && !uploadExpiresAt.isAfter(Instant.now(clock));
    }

    public boolean isOwnedBy(UUID userId) {
        return Objects.equals(ownerUserId, userId);
    }

    public boolean belongsToWorkspace(UUID workspaceId) {
        return Objects.equals(this.workspaceId, workspaceId);
    }

    public boolean isStaticDisplayAsset() {
        return purpose == FileAssetPurpose.USER_AVATAR
                || purpose == FileAssetPurpose.USER_COVER
                || purpose == FileAssetPurpose.WORKSPACE_LOGO;
    }

    public void markUploaded(Long actualSizeBytes, String storageVersionId, String storageMetadata, Clock clock) {
        requireActive("FileAsset");
        Assert.notNull(clock, "clock is required");

        if (status == FileAssetStatus.UPLOADED) {
            return;
        }

        Assert.state(status == FileAssetStatus.PENDING_UPLOAD, "file asset is not pending upload");
        Assert.state(!isUploadExpired(clock), "file asset upload has expired");

        this.actualSizeBytes = actualSizeBytes;
        this.storageVersionId = requireText(storageVersionId, "storageVersionId");
        this.storageMetadata = normalizeJsonOrEmpty(storageMetadata);
        this.status = FileAssetStatus.UPLOADED;
        this.uploadedAt = Instant.now(clock);
    }

    public void markDeleted(Clock clock) {
        Assert.notNull(clock, "clock is required");

        if (isDeleted()) {
            return;
        }

        deletedFlag = true;
        status = FileAssetStatus.DELETED;
        softDelete(clock);
    }

    public void expirePendingUpload(Clock clock) {
        Assert.notNull(clock, "clock is required");
        Assert.state(isPendingUpload(), "file asset is not pending upload");
        Assert.state(isUploadExpired(clock), "file asset upload has not expired");
        markDeleted(clock);
    }

    public UUID claimStorageCleanup(Clock clock, Duration claimLease) {
        Assert.notNull(clock, "clock is required");
        Assert.notNull(claimLease, "claimLease is required");
        Assert.isTrue(!claimLease.isZero() && !claimLease.isNegative(), "claimLease must be positive");
        Assert.state(isDeleted(), "only deleted file assets may be claimed for storage cleanup");
        Assert.state(isUploadExpired(clock), "storage cleanup must wait until the upload credential has expired");
        Assert.state(storageDeletedAt == null, "storage object is already deleted");

        Instant now = Instant.now(clock);
        boolean activeClaim = storageCleanupClaimedUntil != null && storageCleanupClaimedUntil.isAfter(now);
        Assert.state(!activeClaim, "storage cleanup claim is still active");

        storageCleanupToken = UUID.randomUUID();
        storageCleanupClaimedUntil = now.plus(claimLease);
        return storageCleanupToken;
    }

    public boolean storageCleanupClaimMatches(UUID claimToken) {
        return claimToken != null && claimToken.equals(storageCleanupToken);
    }

    public void markStorageDeleted(UUID claimToken, Clock clock) {
        Assert.notNull(claimToken, "claimToken is required");
        Assert.notNull(clock, "clock is required");
        Assert.state(storageCleanupClaimMatches(claimToken), "storage cleanup claim does not match");

        storageDeletedAt = Instant.now(clock);
        storageCleanupToken = null;
        storageCleanupClaimedUntil = null;
    }

    private void initializeForCreate(FileAssetCreateParam param) {
        assignId(param.getId());
        workspaceId = param.getWorkspaceId();
        ownerUserId = param.getOwnerUserId();
        storageProvider = param.getStorageProvider();
        bucket = param.getBucket();
        objectKey = param.getObjectKey();
        originalFilename = param.getOriginalFilename();
        contentType = param.getContentType();
        extension = param.getExtension();
        declaredSizeBytes = param.getDeclaredSizeBytes();
        purpose = param.getPurpose();
        uploadExpiresAt = param.getUploadExpiresAt();
        status = FileAssetStatus.PENDING_UPLOAD;
        deletedFlag = false;
        attributes = "{}";
        storageMetadata = "{}";
    }

    private static String normalizeJsonOrEmpty(String value) {
        if (!StringUtils.hasText(value)) {
            return "{}";
        }

        return value.trim();
    }

    private static String requireText(String value, String fieldName) {
        Assert.hasText(value, fieldName + " is required");
        return value.trim();
    }

}
