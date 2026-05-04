package com.syncturtle.common.contracts.instance.admin;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AdminSigninRequest {
    String email;
    String password;
    String clientIp;
    String userAgent;
}
