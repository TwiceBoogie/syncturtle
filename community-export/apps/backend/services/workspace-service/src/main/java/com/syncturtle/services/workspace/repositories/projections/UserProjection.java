package com.syncturtle.services.workspace.repositories.projections;

import java.time.Instant;
import java.util.UUID;

public interface UserProjection {
    UUID getId();

    String getUsername();

    String getEmail();

    String getDisplayName();

    String getFirstName();

    String getLastName();

    Instant getDateJoined();

    UUID getAvatarAssetId();

    UUID getCoverImageAssetId();

    boolean isActive();

    boolean isEmailVerified();

    boolean isPasswordAutoset();

    String getUserTimezone();

    boolean isBot();
}
