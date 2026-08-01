package com.syncturtle.services.file.model.param;

import java.util.UUID;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import lombok.Builder;
import lombok.Getter;

@Getter
public class FileAssetLinkCreateParam {

    private static final int MAX_TARGET_SERVICE_LENGTH = 64;
    private static final int MAX_TARGET_TYPE_LENGTH = 64;
    private static final int MAX_USAGE_TYPE_LENGTH = 64;

    private final UUID assetId;
    private final UUID workspaceId;
    private final String targetService;
    private final String targetType;
    private final UUID targetId;
    private final String usageType;
    private final boolean primary;
    private final Long sourceVersion;
    private final UUID linkedByUserId;
    private final String attributes;

    @Builder
    private FileAssetLinkCreateParam(
            UUID assetId,
            UUID workspaceId,
            String targetService,
            String targetType,
            UUID targetId,
            String usageType,
            boolean primary,
            Long sourceVersion,
            UUID linkedByUserId,
            String attributes) {
        Assert.notNull(targetId, "targetId is required");
        Assert.notNull(sourceVersion, "sourceVersion is required");

        this.assetId = assetId;
        this.workspaceId = workspaceId;
        this.targetService = normalizeRequired(targetService, "targetService", MAX_TARGET_SERVICE_LENGTH);
        this.targetType = normalizeRequired(targetType, "targetType", MAX_TARGET_TYPE_LENGTH);
        this.targetId = targetId;
        this.usageType = normalizeRequired(usageType, "usageType", MAX_USAGE_TYPE_LENGTH);
        this.primary = primary;
        this.sourceVersion = sourceVersion;
        this.linkedByUserId = linkedByUserId;
        this.attributes = normalizeJsonOrDefault(attributes);
    }

    private static String normalizeRequired(String value, String fieldName, int maxLength) {
        Assert.hasText(value, fieldName + " is required");

        String normalized = value.trim();

        Assert.isTrue(normalized.length() <= maxLength, fieldName + " must be " + maxLength + " characters or fewer");

        return normalized;
    }

    private static String normalizeJsonOrDefault(String value) {
        if (!StringUtils.hasText(value)) {
            return "{}";
        }

        return value.trim();
    }

}