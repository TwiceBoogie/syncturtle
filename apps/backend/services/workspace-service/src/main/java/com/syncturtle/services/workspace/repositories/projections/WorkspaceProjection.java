package com.syncturtle.services.workspace.repositories.projections;

import java.time.Instant;
import java.util.UUID;

public interface WorkspaceProjection {
    UUID getId();

    String getName();

    String getLogo();

    UUID getLogoAssetId();

    String getSlug();

    String getOrganizationSize();

    UserProjection getOwner();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    UUID getCreatedById();

    UUID getUpdatedById();
}
