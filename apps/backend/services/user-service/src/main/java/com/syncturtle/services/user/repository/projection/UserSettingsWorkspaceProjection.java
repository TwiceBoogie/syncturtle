package com.syncturtle.services.user.repository.projection;

import java.util.UUID;

public interface UserSettingsWorkspaceProjection {
    UUID getId();

    String getSlug();

    String getName();

    UUID getLogoAssetId();
}
