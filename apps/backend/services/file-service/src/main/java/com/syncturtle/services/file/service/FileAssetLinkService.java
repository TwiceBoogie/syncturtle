package com.syncturtle.services.file.service;

import java.util.UUID;

import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;

public interface FileAssetLinkService {
    void upsertPrimaryWorkspaceLogoLink(WorkspaceEvent event);

    void removePrimaryWorkspaceLogoLink(UUID workspaceId, Long sourceVersion);

    void upsertPrimaryUserAvatarLink(UserEvent event);

    void upsertPrimaryUserCoverLink(UserEvent event);

    void removePrimaryUserAvatarLink(UUID userId, Long sourceVersion);

    void removePrimaryUserCoverLink(UUID userId, Long sourceVersion);

}
