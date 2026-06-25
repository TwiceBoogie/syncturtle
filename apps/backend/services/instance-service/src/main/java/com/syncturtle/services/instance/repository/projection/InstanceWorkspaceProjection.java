package com.syncturtle.services.instance.repository.projection;

import java.time.Instant;
import java.util.UUID;

public interface InstanceWorkspaceProjection {
    UUID getId();

    String getName();

    UUID getLogoAssetId();

    String getSlug();

    String getOrganizationSize();

    String getTimezone();

    UUID getCreatedById();

    UUID getUpdatedById();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    Long getTotalMembers();

    UUID getOwnerId();

    String getOwnerUsername();

    String getOwnerEmail();

    String getOwnerDisplayName();

    String getOwnerFirstName();

    String getOwnerLastName();

    Instant getOwnerCreatedAt();

    UUID getOwnerAvatarAssetId();

    UUID getOwnerCoverImageAssetId();

    Boolean getOwnerEmailVerified();

    Boolean getOwnerPasswordAutoset();

    String getOwnerUserTimezone();

    String getOwnerPrincipalType();
}
