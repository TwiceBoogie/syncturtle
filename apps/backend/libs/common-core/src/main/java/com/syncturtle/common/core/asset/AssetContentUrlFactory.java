package com.syncturtle.common.core.asset;

import java.util.UUID;

public class AssetContentUrlFactory {

    private static final String STATIC_ASSET_PREFIX = "/api/assets/v1/static/";

    public String staticAssetUrl(UUID assetId) {
        if (assetId == null) {
            return null;
        }

        return STATIC_ASSET_PREFIX + assetId;
    }

    public String requiredStaticAssetUrl(UUID assetId) {
        String url = staticAssetUrl(assetId);

        if (url == null) {
            throw new IllegalArgumentException("assetId is required");
        }

        return url;
    }

}
