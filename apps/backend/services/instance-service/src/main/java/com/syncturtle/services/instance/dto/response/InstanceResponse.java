package com.syncturtle.services.instance.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InstanceResponse {
    UUID id;
    String instanceName;
    String whitelistEmails;
    String licenseKey;
    String instanceId;
    String currentVersion;
    String latestVersion;
    Instant lastCheckedAt;
    String namespace;
    @JsonProperty("isTelemetryEnabled")
    boolean telemetryEnabled;
    @JsonProperty("isSupportRequired")
    boolean supportRequired;
    @JsonProperty("isActivated")
    boolean activated;
    @JsonProperty("isSetupDone")
    boolean setupDone;
    @JsonProperty("isSignupScreenVisited")
    boolean signupScreenVisited;
    @JsonProperty("isVerified")
    boolean verified;
    boolean workspaceExist;
    long userCount;
    Instant createdAt;
    Instant updatedAt;
    UUID createdBy;
    UUID updatedBy;
}
