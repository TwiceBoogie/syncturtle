package com.syncturtle.common.contracts.instance.admin;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public final class AdminSignupRequest {
    String firstName;
    String lastName;
    String email;
    String companyName;
    boolean telemetryEnabled;
    String password;
    String clientIp;
    String userAgent;
}
