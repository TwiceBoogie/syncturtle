package com.syncturtle.platform.services.instance.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public final class InstanceRequest {
    private String instanceName;
    @JsonProperty("isTelemetryEnabled")
    private Boolean telemetryEnabled;
}
