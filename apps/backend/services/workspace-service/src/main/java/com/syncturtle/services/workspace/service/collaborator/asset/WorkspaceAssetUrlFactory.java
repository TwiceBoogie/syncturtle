package com.syncturtle.services.workspace.service.collaborator.asset;

import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class WorkspaceAssetUrlFactory {

    public String workspaceLogoContentUrl(UUID logoAssetId) {
        if (logoAssetId == null) {
            return null;
        }
        return "/api/assets/v1/static/" + logoAssetId.toString();
    }

}
