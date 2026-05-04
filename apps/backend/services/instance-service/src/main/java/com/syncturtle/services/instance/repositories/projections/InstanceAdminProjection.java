package com.syncturtle.services.instance.repositories.projections;

import java.time.Instant;
import java.util.UUID;

public interface InstanceAdminProjection {
    UUID getId();

    int getRole();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    UUID getCreatedById();

    UUID getUpdatedById();

    InstanceIdProjection getInstance();

    default UUID getInstanceId() {
        return getInstance() != null ? getInstance().getId() : null;
    }

    UUID getUserId();

    UserLiteProjection getUser();
}
