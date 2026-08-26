package com.syncturtle.services.file.service;

import java.util.UUID;

import com.syncturtle.services.file.dto.response.StaticAssetUrlResponse;

public interface StaticAssetService {
    StaticAssetUrlResponse signedStaticAssetUrl(UUID currentUserId, UUID assetId);
}
