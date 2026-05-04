package com.syncturtle.platform.gateway.support;

import java.time.Instant;

public record PassportSessionRecord(
        String sessionId,
        String userId,
        String instanceId,
        boolean active,
        Instant expiresAt) {

}
