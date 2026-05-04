package com.syncturtle.services.user.dto.internal;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConsumedBootstrapGrant {
    private final String userId;
    private final String instanceId;
    private final Long authVersion;
    private final Long adminSessionVersion;
    private final List<String> roles;
}
