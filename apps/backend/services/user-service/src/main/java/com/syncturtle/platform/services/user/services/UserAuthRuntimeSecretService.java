package com.syncturtle.platform.services.user.services;

import com.syncturtle.platform.services.user.dto.internal.UserAuthRuntimeSecretConfig;

public interface UserAuthRuntimeSecretService {
    UserAuthRuntimeSecretConfig get();

    void evict();
}
