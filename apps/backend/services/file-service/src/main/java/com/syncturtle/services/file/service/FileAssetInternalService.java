package com.syncturtle.services.file.service;

import java.util.UUID;

import com.syncturtle.services.file.dto.response.FileAssetValidationResponse;

public interface FileAssetInternalService {
    FileAssetValidationResponse validateAsset(UUID assetId);
}
