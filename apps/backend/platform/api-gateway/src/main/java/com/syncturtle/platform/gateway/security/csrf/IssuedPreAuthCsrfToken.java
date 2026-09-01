package com.syncturtle.platform.gateway.security.csrf;

import java.time.Instant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class IssuedPreAuthCsrfToken {
    private final String submittedToken;
    private final String signedCookieToken;
    private final Instant issuedAt;
    private final Instant expiresAt;
}
