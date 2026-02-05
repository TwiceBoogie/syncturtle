package com.syncturtle.platform.services.user.services;

import java.util.Map;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;

public interface FeatureFlagService {
    Map<InstanceConfigurationKey, String> getInstanceConfigurations();

    String get(InstanceConfigurationKey key);
}
