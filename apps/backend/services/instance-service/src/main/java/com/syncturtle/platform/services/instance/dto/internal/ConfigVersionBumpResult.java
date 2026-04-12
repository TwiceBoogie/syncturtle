package com.syncturtle.platform.services.instance.dto.internal;

import java.util.Map;

import com.syncturtle.common.core.enums.InstanceConfigScope;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class ConfigVersionBumpResult {
    private final long globalVersion;
    private final Map<InstanceConfigScope, Long> scopeVersions;
}
