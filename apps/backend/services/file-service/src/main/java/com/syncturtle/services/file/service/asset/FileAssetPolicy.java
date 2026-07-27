package com.syncturtle.services.file.service.asset;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.file.FileAssetPurpose;
import com.syncturtle.common.contracts.file.error.FileErrorCode;
import com.syncturtle.common.contracts.file.exception.FileException;
import com.syncturtle.services.file.model.FileAsset;

@Component
public class FileAssetPolicy {

    private static final long DEFAULT_MAX_IMAGE_BYTES = 5L * 1024L * 1024L;

    public void requireCreateAllowed(FileAssetPurpose purpose, UUID workspaceId, String contentType, long sizeBytes) {
        Assert.notNull(purpose, "purpose is required");
        Assert.notNull(contentType, "contentType is required");

        if (purpose == FileAssetPurpose.WORKSPACE_LOGO && workspaceId == null) {
            throw FileException.uploadScopeInvalid("workspaceId is required for workspace logo uploads");
        }

        if ((purpose == FileAssetPurpose.USER_AVATAR || purpose == FileAssetPurpose.USER_COVER)
                && workspaceId != null) {
            throw FileException.uploadScopeInvalid("workspaceId must be null for user assets");
        }

        if (sizeBytes <= 0) {
            throw FileException.uploadScopeInvalid("sizeBytes must be greater than 0");
        }

        if (sizeBytes > DEFAULT_MAX_IMAGE_BYTES) {
            throw FileException.assetTooLarge(sizeBytes, DEFAULT_MAX_IMAGE_BYTES);
        }

        requireAllowedImageContentType(contentType);
    }

    public void requireOwner(FileAsset asset, UUID currentUserId) {
        Assert.notNull(asset, "file asset is required");
        Assert.notNull(currentUserId, "currentUserId is required");

        if (!asset.isOwnedBy(currentUserId)) {
            throw FileException.assetOwnerMismatch(asset.getId(), currentUserId);
        }
    }

    public void requireUploaded(FileAsset asset) {
        Assert.notNull(asset, "file asset is required");

        if (!asset.isUploaded()) {
            throw FileException.assetNotUploaded(asset.getId());
        }
    }

    public void requireStaticDisplayAsset(FileAsset asset) {
        Assert.notNull(asset, "file asset is required");

        requireUploaded(asset);

        if (!asset.isStaticDisplayAsset()) {
            throw FileException.assetNotStaticDisplayAsset(asset.getId());
        }
    }

    public void requirePurpose(FileAsset asset, FileAssetPurpose purpose) {
        Assert.notNull(asset, "file asset is required");
        Assert.notNull(purpose, "purpose is required");

        if (asset.getPurpose() != purpose) {
            throw FileException.of(FileErrorCode.FILE_ASSET_PURPOSE_INVALID)
                    .with("asset_id", asset.getId())
                    .with("expected_purpose", purpose)
                    .with("actual_purpose", asset.getPurpose());
        }
    }

    public void requireWorkspace(FileAsset asset, UUID workspaceId) {
        Assert.notNull(asset, "file asset is required");
        Assert.notNull(workspaceId, "workspaceId is required");

        if (!asset.belongsToWorkspace(workspaceId)) {
            throw FileException.assetWorkspaceMismatch(asset.getId(), workspaceId);
        }
    }

    public void requireStorageObjectMatchesDeclaration(FileAsset asset, Map<String, Object> metadata) {
        Assert.notNull(asset, "asset is required");
        Assert.notNull(metadata, "metadata is required");

        Object contentLength = metadata.get("ContentLength");
        if (contentLength instanceof Number number) {
            long actualSizeBytes = number.longValue();

            if (actualSizeBytes <= 0 || actualSizeBytes > asset.getDeclaredSizeBytes()) {
                throw FileException.assetStorageMetadataInvalid(asset.getId());
            }
        }

        Object contentType = metadata.get("ContentType");
        if (contentType instanceof String actualContentType && StringUtils.hasText(actualContentType)) {
            if (!Objects.equals(actualContentType.trim(), asset.getContentType())) {
                throw FileException.assetStorageMetadataInvalid(asset.getId());
            }
        }
    }

    public static void requireAllowedImageContentType(String contentType) {
        String normalized = contentType.trim().toLowerCase();

        boolean allowed = normalized.equals("image/jpeg")
                || normalized.equals("image/jpg")
                || normalized.equals("image/png")
                || normalized.equals("image/webp")
                || normalized.equals("image/gif");

        if (!allowed) {
            throw FileException.unsupportedContentType(contentType);
        }
    }

}
