package com.syncturtle.services.user.services;

import com.syncturtle.services.user.dto.internal.UserAuthRuntimeSecretConfig;

public interface UserAuthRuntimeSecretService {
    UserAuthRuntimeSecretConfig get();

    void evict();
}
