package com.syncturtle.services.workspace.service.asset;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.file.FileAssetPurpose;
import com.syncturtle.common.contracts.workspace.exception.WorkspaceException;
import com.syncturtle.services.workspace.client.FileClient;
import com.syncturtle.services.workspace.dto.response.FileAssetValidationResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WorkspaceLogoAssetVerifier {

    private final FileClient fileClient;

    public void requireValidLogoAsset(UUID assetId, UUID workspaceId, UUID currentUserId) {
        Assert.notNull(assetId, "assetId is required");
        Assert.notNull(workspaceId, "workspaceId is required");
        Assert.notNull(currentUserId, "currentUserId is required");

        FileAssetValidationResponse asset = fileClient.validateAsset(assetId);

        if (asset == null || asset.isDeleted()) {
            throw WorkspaceException.logoAssetInvalid(assetId);
        }

        if (!asset.isUploaded()) {
            throw WorkspaceException.logoAssetNotUploaded(assetId);
        }

        if (asset.getPurpose() != FileAssetPurpose.WORKSPACE_LOGO) {
            throw WorkspaceException.logoAssetForbidden(assetId, workspaceId);
        }

        if (!workspaceId.equals(asset.getWorkspaceId())) {
            throw WorkspaceException.logoAssetForbidden(assetId, workspaceId);
        }

        if (!currentUserId.equals(asset.getOwnerUserId())) {
            throw WorkspaceException.logoAssetForbidden(assetId, workspaceId);
        }
    }

}
