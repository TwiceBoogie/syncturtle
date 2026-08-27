package com.syncturtle.services.user.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserSessionResponse {
    UUID sessionId;
    boolean current;
    boolean administrator;
    Instant createdAt;
    Instant lastUsedAt;
    Instant idleExpiresAt;
    Instant absoluteExpiresAt;
}
