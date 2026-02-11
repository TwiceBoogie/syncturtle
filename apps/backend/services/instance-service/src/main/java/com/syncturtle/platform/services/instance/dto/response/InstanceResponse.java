package com.syncturtle.platform.services.instance.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public final class InstanceResponse {
    private UUID id;
    private String instanceName;
    private String whitelistEmails;
    private String licenseKey;
    private String instanceId;
    private String currentVersion;
    private String latestVersion;
    private Instant lastCheckedAt;
    private String namespace;
    @JsonProperty("isTelemetryEnabled")
    private boolean telemetryEnabled;
    @JsonProperty("isSupportRequired")
    private boolean supportRequired;
    @JsonProperty("isActivated")
    private boolean activated;
    @JsonProperty("isSetupDone")
    private boolean setupDone;
    @JsonProperty("isSignupScreenVisited")
    private boolean signupScreenVisited;
    private long userCount;
    @JsonProperty("isVerified")
    private boolean verified;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private boolean workspaceExist;
}
