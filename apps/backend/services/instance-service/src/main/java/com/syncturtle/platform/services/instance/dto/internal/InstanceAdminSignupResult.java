package com.syncturtle.platform.services.instance.dto.internal;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class InstanceAdminSignupResult {
    private final UUID userId;
    private final Long authVersion;
    private final Long adminSessionVersion;
    private final UUID instanceId;
    private final String rolesCsv;
    private final String redirectLocation;
}
