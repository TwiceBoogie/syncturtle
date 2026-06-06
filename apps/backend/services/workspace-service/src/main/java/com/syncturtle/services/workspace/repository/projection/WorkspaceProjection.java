package com.syncturtle.services.workspace.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface WorkspaceProjection {
    UUID getId();

    String getName();

    UUID getLogoAssetId();

    String getSlug();

    String getOrganizationSize();

    UserProjection getOwner();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    UUID getCreatedById();

    UUID getUpdatedById();
}
