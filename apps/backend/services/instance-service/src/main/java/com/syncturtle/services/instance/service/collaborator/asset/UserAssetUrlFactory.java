package com.syncturtle.services.instance.service.collaborator.asset;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public final class UserAssetUrlFactory {

    public String avatarContentUrl(UUID avatarAssetId) {
        if (avatarAssetId == null) {
            return null;
        }

        return "/api/files/assets/" + avatarAssetId + "/content";
    }

    public String coverImageContentUrl(UUID coverImageAssetId) {
        if (coverImageAssetId == null) {
            return null;
        }

        return "/api/files/assets/" + coverImageAssetId + "/content";
    }

}
