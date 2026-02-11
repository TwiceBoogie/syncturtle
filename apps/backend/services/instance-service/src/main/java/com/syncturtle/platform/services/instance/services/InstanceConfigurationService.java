package com.syncturtle.platform.services.instance.services;

import java.util.Map;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;

public interface InstanceConfigurationService {
    Map<InstanceConfigurationKey, String> configurations();
}
