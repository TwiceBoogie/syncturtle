package com.syncturtle.platform.services.user.services;

import com.syncturtle.platform.services.user.dto.internal.UserAuthRuntimeConfig;

public interface FeatureFlagService {
    UserAuthRuntimeConfig getInstanceConfigurations();

    void evict();
}
