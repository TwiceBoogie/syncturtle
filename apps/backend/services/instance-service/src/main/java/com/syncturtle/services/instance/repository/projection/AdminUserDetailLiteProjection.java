package com.syncturtle.services.instance.repository.projection;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.core.actor.PrincipalType;

public interface AdminUserDetailLiteProjection {
    UUID getId();

    String getEmail();

    String getDisplayName();

    String getFirstName();

    String getLastName();

    UUID getAvatarAssetId();

    PrincipalType getPrincipalType();

    Instant getCreatedAt();

    default boolean isBot() {
        return getPrincipalType() == PrincipalType.BOT;
    }
}
