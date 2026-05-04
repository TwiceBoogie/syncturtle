package com.syncturtle.services.instance.dto.internal;

import java.util.Map;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScopeNames;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class ConfigVersionBumpResult {
    private final long globalVersion;
    private final Map<InstanceConfigurationScopeNames, Long> scopeVersions;
}
