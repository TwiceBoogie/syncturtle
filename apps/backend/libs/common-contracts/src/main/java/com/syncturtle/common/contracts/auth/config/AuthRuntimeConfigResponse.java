package com.syncturtle.common.contracts.auth.config;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AuthRuntimeConfigResponse {
    boolean signupEnabled;
    boolean magicLinkEnabled;
    boolean emailPasswordEnabled;
    boolean smtpEnabled;
    Long scopeVersion;
    Long globalVersion;
}
