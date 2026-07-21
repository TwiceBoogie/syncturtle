package com.syncturtle.services.file.model.param;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.file.FileAssetPurpose;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class FileAssetCreateParam {

    private static final int MAX_STORAGE_PROVIDER_LENGTH = 32;
    private static final int MAX_BUCKET_LENGTH = 255;
    private static final int MAX_OBJECT_KEY_LENGTH = 1000;
    private static final int MAX_FILENAME_LENGTH = 255;
    private static final int MAX_CONTENT_TYPE_LENGTH = 127;
    private static final int MAX_EXTENSION_LENGTH = 20;

    private final UUID id;
    private final UUID workspaceId;
    private final UUID ownerUserId;
    private final String storageProvider;
    private final String bucket;
    private final String objectKey;
    private final String originalFilename;
    private final String contentType;
    private final String extension;
    private final long declaredSizeBytes;
    private final FileAssetPurpose purpose;
    private final Instant uploadExpiresAt;

    @Builder
    private FileAssetCreateParam(
            UUID id,
            UUID workspaceId,
            UUID ownerUserId,
            String storageProvider,
            String bucket,
            String objectKey,
            String originalFilename,
            String contentType,
            String extension,
            long declaredSizeBytes,
            FileAssetPurpose purpose,
            Instant uploadExpiresAt) {
        Assert.notNull(id, "id is required");
        Assert.notNull(ownerUserId, "ownerUserId is required");
        Assert.notNull(purpose, "purpose is required");
        Assert.notNull(uploadExpiresAt, "uploadExpiresAt is required");
        Assert.isTrue(declaredSizeBytes > 0, "declaredSizeBytes must be greater than 0");

        this.id = id;
        this.workspaceId = workspaceId;
        this.ownerUserId = ownerUserId;
        this.storageProvider = normalizeRequired(storageProvider, "storageProvider", MAX_STORAGE_PROVIDER_LENGTH);
        this.bucket = normalizeRequired(bucket, "bucket", MAX_BUCKET_LENGTH);
        this.objectKey = normalizeRequired(objectKey, "objectKey", MAX_OBJECT_KEY_LENGTH);
        this.originalFilename = normalizeRequired(originalFilename, "originalFilename", MAX_FILENAME_LENGTH);
        this.contentType = normalizeRequired(contentType, "contentType", MAX_CONTENT_TYPE_LENGTH);
        this.extension = normalizeExtension(extension);
        this.declaredSizeBytes = declaredSizeBytes;
        this.purpose = purpose;
        this.uploadExpiresAt = uploadExpiresAt;
    }

    private static String normalizeExtension(String value) {
        String normalized = normalizeNullable(value, "extension", MAX_EXTENSION_LENGTH);

        if (normalized == null) {
            return null;
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

    private static String normalizeNullable(String value, String fieldName, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

}