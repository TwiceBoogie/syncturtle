package com.syncturtle.common.contracts.auth.authz;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResolveInstanceAuthorizationRequest {
    private final UUID userId;
    private final UUID instanceId;
}
