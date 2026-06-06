package com.syncturtle.services.instance.service.asset;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public final class WorkspaceAssetUrlFactory {

    public String logoContentUrl(UUID logoAssetId) {
        if (logoAssetId == null) {
            return null;
        }

        return "/api/files/assets/" + logoAssetId + "/content";
    }

}
