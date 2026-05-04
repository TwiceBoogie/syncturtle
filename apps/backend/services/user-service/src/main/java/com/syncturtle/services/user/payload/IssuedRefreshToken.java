package com.syncturtle.services.user.payload;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IssuedRefreshToken {
    String sessionId;
    String token;
    Instant issuedAt;
    Instant expiresAt;
}
