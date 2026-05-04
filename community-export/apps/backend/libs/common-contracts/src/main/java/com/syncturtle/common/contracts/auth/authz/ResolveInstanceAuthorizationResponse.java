package com.syncturtle.common.contracts.auth.authz;

import java.util.UUID;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ResolveInstanceAuthorizationResponse {
    UUID userId;
    UUID instanceId;
    boolean instanceAdmin;
    Long adminSessionVersion;
}
