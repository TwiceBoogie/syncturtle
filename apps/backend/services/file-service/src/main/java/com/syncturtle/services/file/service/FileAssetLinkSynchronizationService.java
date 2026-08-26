package com.syncturtle.services.file.service;

import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;

public interface FileAssetLinkSynchronizationService {
    void synchronizeUserAssetLinks(UserEvent event);

    void synchronizeWorkspaceAssetLinks(WorkspaceEvent event);
}
