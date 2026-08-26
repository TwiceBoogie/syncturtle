package com.syncturtle.services.file.service.param;

import java.util.UUID;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.file.FileAssetPurpose;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class FileUploadCreateParam {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 128;

    private final UUID currentUserId;
    private final String idempotencyKey;
    private final FileAssetPurpose purpose;
    private final UUID workspaceId;
    private final String originalFilename;
    private final String contentType;
    private final long sizeBytes;

    @Builder
    private FileUploadCreateParam(
            UUID currentUserId,
            String idempotencyKey,
            FileAssetPurpose purpose,
            UUID workspaceId,
            String originalFilename,
            String contentType,
            long sizeBytes) {
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(purpose, "purpose is required");
        Assert.hasText(originalFilename, "originalFilename is required");
        Assert.hasText(contentType, "contentType is required");
        Assert.isTrue(sizeBytes > 0, "sizeBytes must be greater than 0");

        this.currentUserId = currentUserId;
        this.idempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        this.purpose = purpose;
        this.workspaceId = workspaceId;
        this.originalFilename = originalFilename.trim();
        this.contentType = contentType.trim();
        this.sizeBytes = sizeBytes;
    }

    public boolean hasIdempotencyKey() {
        return idempotencyKey != null;
    }

    private static String normalizeIdempotencyKey(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= MAX_IDEMPOTENCY_KEY_LENGTH,
                "idempotencyKey must be 128 characters or fewer");
        Assert.isTrue(!normalized.contains(":"), "idempotencyKey must not contain ':'");
        Assert.isTrue(!normalized.contains(" "), "idempotencyKey must not contain spaces");

        return normalized;
    }

}
