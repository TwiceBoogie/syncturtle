package com.syncturtle.services.user.services;

import com.syncturtle.services.user.dto.internal.UserAuthRuntimeConfig;

public interface FeatureFlagService {
    UserAuthRuntimeConfig getInstanceConfigurations();

    void evict();
}
