package com.syncturtle.platform.services.instance.dto.internal;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class InstanceAdminSignupResult {
    private final UUID userId;
    private final String redirectLocation;
}
