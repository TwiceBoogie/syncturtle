package com.syncturtle.platform.gateway.security.session;

import java.time.Instant;
import java.util.List;

import lombok.Getter;

@Getter
public final class ParsedPassportSession {

    private final int recordVersion;
    private final String sessionId;
    private final String userId;
    private final String instanceId;
    private final List<String> roles;
    private final long userAuthVersion;
    private final Long adminSessionVersion;
    private final boolean active;
    private final Instant issuedAt;
    private final Instant expiresAt;

    public ParsedPassportSession(
            int recordVersion,
            String sessionId,
            String userId,
            String instanceId,
            List<String> roles,
            long userAuthVersion,
            Long adminSessionVersion,
            boolean active,
            Instant issuedAt,
            Instant expiresAt) {
        this.recordVersion = recordVersion;
        this.sessionId = sessionId;
        this.userId = userId;
        this.instanceId = instanceId;
        this.roles = List.copyOf(roles);
        this.userAuthVersion = userAuthVersion;
        this.adminSessionVersion = adminSessionVersion;
        this.active = active;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }
}
