package com.syncturtle.services.instance.repository.projection;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.core.actor.PrincipalType;

public interface InstanceWorkspaceOwnerProjection {
    UUID getId();

    String getUsername();

    String getEmail();

    String getDisplayName();

    String getFirstName();

    String getLastName();

    Instant getCreatedAt();

    UUID getAvatarAssetId();

    UUID getCoverImageAssetId();

    boolean isEmailVerified();

    boolean isPasswordAutoset();

    String getUserTimezone();

    PrincipalType getPrincipalType();

    default boolean isBot() {
        return getPrincipalType() == PrincipalType.BOT;
    }
}
