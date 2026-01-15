package com.syncturtle.platform.services.instance.repositories.projections;

import java.time.Instant;
import java.util.UUID;

public interface UserLiteProjection {
    UUID getId();

    String getEmail();

    String getDisplayName();

    String getFirstName();

    String getLastName();

    UUID getAvatarAssetId();

    boolean isBot();

    Instant getDateJoined();
}
