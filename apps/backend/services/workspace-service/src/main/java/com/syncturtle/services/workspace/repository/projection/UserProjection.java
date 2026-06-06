package com.syncturtle.services.workspace.repository.projection;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.core.actor.PrincipalType;

public interface UserProjection {
    UUID getId();

    String getUsername();

    String getEmail();

    String getDisplayName();

    String getFirstName();

    String getLastName();

    Instant getCreatedAt();

    UUID getAvatarAssetId();

    UUID getCoverImageAssetId();

    boolean isActive();

    boolean isEmailVerified();

    boolean isPasswordAutoset();

    String getUserTimezone();

    PrincipalType getPrincipalType();

    default boolean isBot() {
        return getPrincipalType() == PrincipalType.BOT;
    }
}
