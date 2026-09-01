package com.syncturtle.platform.gateway.dto.response;

import java.time.Instant;

import com.syncturtle.platform.gateway.type.GatewayCsrfScope;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class CsrfTokenResponse {
    private final String csrfToken;
    private final GatewayCsrfScope scope;
    private final Instant expiresAt;
}
