package com.syncturtle.services.instance.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;

@Getter
public final class InstanceNotConfiguredResponse implements InstanceInfo {
    @JsonProperty("isActivated")
    private final boolean activated;
    @JsonProperty("isSetupDone")
    private final boolean setupDone;

    public InstanceNotConfiguredResponse() {
        this.activated = false;
        this.setupDone = false;
    }
}
