package com.syncturtle.common.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class AdminSignupInternalRequest {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String companyName;
    private final boolean telemetryEnabled;
    private final String password;
}
