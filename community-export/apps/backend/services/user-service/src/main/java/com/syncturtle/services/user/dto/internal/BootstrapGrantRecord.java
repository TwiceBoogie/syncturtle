package com.syncturtle.services.user.dto.internal;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BootstrapGrantRecord {
    private final String purpose;
    private final String userId;
    private final String instanceId;
    private final Long authVersion;
    private final Long adminSessionVersion;
    private final List<String> roles;
    private final String clientIp;
    private final String userAgent;
    private final Instant issuedAt;
    private final Instant expiresAt;
}
