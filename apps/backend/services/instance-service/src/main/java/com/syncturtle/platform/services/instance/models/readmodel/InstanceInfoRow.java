package com.syncturtle.platform.services.instance.models.readmodel;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstanceInfoRow {
    private final UUID id;
    private final String instanceName;
    private final String whitelistEmails;
    private final String instanceId;

    private final String currentVersion;
    private final String latestVersion;
    private final Instant lastCheckedAt;

    private final String namespace;

    private final boolean telemetryEnabled;
    private final boolean supportRequired;
    private final boolean setupDone;
    private final boolean signupScreenVisited;
    private final boolean verified;

    private final Instant createdAt;
    private final Instant updatedAt;
    private final UUID createdById;
    private final UUID updatedById;
}
