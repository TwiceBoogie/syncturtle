package com.syncturtle.platform.services.user.services.impl;

import java.util.Map;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.user.client.InstanceClient;
import com.syncturtle.platform.services.user.services.FeatureFlagService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureFlagServiceImpl implements FeatureFlagService {

    private final InstanceClient instanceClient;

    @Override
    @Cacheable(cacheNames = "s2s:instance:configuration", key = "'latest'", unless = "#result == null || #result.isEmpty()", sync = true)
    public Map<InstanceConfigurationKey, String> getInstanceConfigurations() {
        return instanceClient.getInstanceConfig();
    }

    @Override
    public String get(InstanceConfigurationKey key) {
        return getInstanceConfigurations().getOrDefault(key, "0");
    }

}
