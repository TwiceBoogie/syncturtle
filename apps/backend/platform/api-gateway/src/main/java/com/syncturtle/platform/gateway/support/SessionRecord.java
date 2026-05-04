package com.syncturtle.platform.gateway.support;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.syncturtle.platform.gateway.enums.SessionType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class SessionRecord {
    private UUID userId;
    private SessionType sessionType;
    private List<String> roles = new ArrayList<>();
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant absoluteExpiresAt;
    private boolean revoked;
}
