package com.syncturtle.services.user.service.collaborator.asset;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.file.FileAssetPurpose;
import com.syncturtle.common.contracts.file.response.FileAssetValidationResponse;
import com.syncturtle.common.contracts.user.exception.UserException;
import com.syncturtle.services.user.client.FileClient;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserFileAssetVerifier {

    private final FileClient fileClient;

    public void requireValidUserAsset(UUID assetId, UUID currentUserId, FileAssetPurpose expectedPurpose) {
        Assert.notNull(assetId, "assetId is required");
        Assert.notNull(currentUserId, "currentUserId is required");
        Assert.notNull(expectedPurpose, "expectedPurpose is required");

        FileAssetValidationResponse asset = fileClient.validateAsset(assetId);

        if (asset == null || asset.isDeleted()) {
            throw UserException.assetInvalid(assetId);
        }

        if (!asset.isUploaded()) {
            throw UserException.assetNotUploaded(assetId);
        }

        if (asset.getPurpose() != expectedPurpose) {
            throw UserException.assetForbidden(assetId);
        }

        if (asset.getWorkspaceId() != null) {
            throw UserException.assetForbidden(assetId);
        }

        if (!currentUserId.equals(asset.getOwnerUserId())) {
            throw UserException.assetForbidden(assetId);
        }
    }

}
