package com.syncturtle.services.file.service;

import java.util.UUID;

import com.syncturtle.common.contracts.file.response.FileAssetValidationResponse;

public interface FileAssetInternalService {
    FileAssetValidationResponse validateAsset(UUID assetId);
}
