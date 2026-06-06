package com.syncturtle.services.instance.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InstanceUpdateRequest {
    @Pattern(regexp = "^(?!\\s*$).+", message = "Instance name cannot be blank.")
    @Size(max = 100, message = "Instance name must be 100 characters or fewer.")
    String instanceName;

    Boolean telemetryEnabled;

    @AssertTrue(message = "Update at least one instance setting.")
    public boolean hasAtLeastOneUpdate() {
        return instanceName != null || telemetryEnabled != null;
    }

    public boolean isTelemetryEnabled() {
        return Boolean.TRUE.equals(telemetryEnabled);
    }
}
