package com.syncturtle.platform.services.instance.repositories.projections;

import java.time.Instant;
import java.util.UUID;

public interface InstanceProjection {
    UUID getId();

    String getInstanceName();

    String getWhitelistEmails();

    String getInstanceId();

    String getCurrentVersion();

    String getLatestVersion();

    Instant getLastCheckedAt();

    String getNamespace();

    boolean isTelemetryEnabled();

    boolean isSupportRequired();

    boolean isSetupDone();

    boolean isSignupScreenVisited();

    boolean isVerified();

    Instant getCreatedAt();

    Instant getUpdatedAt();

    UUID getCreatedBy();

    UUID getUpdatedBy();
}
