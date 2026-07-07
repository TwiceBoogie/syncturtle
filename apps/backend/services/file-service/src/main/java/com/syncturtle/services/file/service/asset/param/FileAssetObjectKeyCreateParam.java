package com.syncturtle.services.file.service.asset.param;

import java.util.UUID;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.syncturtle.common.contracts.file.FileAssetPurpose;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class FileAssetObjectKeyCreateParam {

    private static final String DEFAULT_FILENAME = "upload";

    private final UUID assetId;
    private final UUID workspaceId;
    private final UUID ownerUserId;
    private final FileAssetPurpose purpose;
    private final String originalFilename;

    @Builder
    private FileAssetObjectKeyCreateParam(
            UUID assetId,
            UUID workspaceId,
            UUID ownerUserId,
            FileAssetPurpose purpose,
            String originalFilename) {
        Assert.notNull(assetId, "assetId is required");
        Assert.notNull(ownerUserId, "ownerUserId is required");
        Assert.notNull(purpose, "purpose is required");

        this.assetId = assetId;
        this.workspaceId = workspaceId;
        this.ownerUserId = ownerUserId;
        this.purpose = purpose;
        this.originalFilename = safeFilename(originalFilename);
    }

    private static String safeFilename(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT_FILENAME;
        }

        String normalized = value.trim().replace("\\", "/");

        int lastSlash = normalized.lastIndexOf('/');
        String basename = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;

        String safe = basename
                .replaceAll("[^a-zA-Z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^[-.]+", "")
                .replaceAll("[-.]+$", "");

        return StringUtils.hasText(safe) ? safe : DEFAULT_FILENAME;
    }

}
