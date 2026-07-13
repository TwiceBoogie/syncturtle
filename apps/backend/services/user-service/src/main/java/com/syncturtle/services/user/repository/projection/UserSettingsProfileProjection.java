package com.syncturtle.services.user.repository.projection;

import java.util.UUID;

public interface UserSettingsProfileProjection {
    UUID getId();

    UUID getLastWorkspaceId();
}
