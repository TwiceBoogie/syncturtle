package com.syncturtle.services.user.service.asset;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class UserAssetUrlFactory {

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
