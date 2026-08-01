package com.syncturtle.services.file.service;

import java.util.UUID;

public interface StaticAssetService {
    String signedStaticAssetUrl(UUID assetId);
}
