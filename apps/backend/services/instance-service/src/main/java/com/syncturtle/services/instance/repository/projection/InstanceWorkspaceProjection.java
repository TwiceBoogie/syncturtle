package com.syncturtle.services.instance.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface InstanceWorkspaceProjection {
    UUID getId();

    String getName();

    UUID getLogoAssetId();

    String getSlug();

    String getOrganizationSize();

    InstanceWorkspaceOwnerProjection getOwner();

    String getTimezone();

    long getTotalMembers();

    UUID getCreatedById();

    UUID getUpdatedById();

    Instant getCreatedAt();

    Instant getUpdatedAt();
}
