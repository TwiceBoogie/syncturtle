package com.syncturtle.services.instance.model.param;

import org.springframework.util.Assert;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class InstanceRegistrationFlagsParam {

    private final boolean telemetryEnabled;
    private final boolean supportRequired;
    private final boolean test;

    @Builder
    private InstanceRegistrationFlagsParam(
            Boolean telemetryEnabled,
            Boolean supportRequired,
            Boolean test) {
        Assert.notNull(telemetryEnabled, "telemetryEnabled is required");
        Assert.notNull(supportRequired, "supportRequired is required");
        Assert.notNull(test, "test is required");

        this.telemetryEnabled = telemetryEnabled;
        this.supportRequired = supportRequired;
        this.test = test;
    }

}
