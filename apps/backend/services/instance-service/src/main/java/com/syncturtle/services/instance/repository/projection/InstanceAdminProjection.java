package com.syncturtle.services.instance.repository.projection;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.services.instance.type.InstanceAdminRole;

public interface InstanceAdminProjection {
    UUID getId();

    UUID getUserId();

    InstanceAdminRole getRole();

    boolean isVerified();

    InstanceIdProjection getInstance();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    UUID getCreatedById();

    UUID getUpdatedById();

    default UUID getInstanceId() {
        return getInstance().getId();
    }
}
