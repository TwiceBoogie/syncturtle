package com.syncturtle.services.workspace.repository.projection;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.core.actor.PrincipalType;

public interface CurrentUserWorkspaceProjection {
    UUID getId();

    String getName();

    String getSlug();

    int getRole();

    UUID getLogoAssetId();

    String getOrganizationSize();

    long getTotalMembers();

    UUID getOwnerId();

    UUID getOwnerAvatarAssetId();

    String getOwnerDisplayName();

    String getOwnerEmail();

    String getOwnerFirstName();

    String getOwnerLastName();

    PrincipalType getOwnerPrincipalType();

    Instant getOwnerCreatedAt();

    UUID getCreatedById();

    UUID getUpdatedById();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    default boolean isBot() {
        return getOwnerPrincipalType() == PrincipalType.BOT;
    }
}
