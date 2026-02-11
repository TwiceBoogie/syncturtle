package com.syncturtle.platform.services.instance.services;

import java.util.List;
import java.util.Map;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.dto.response.InstanceConfigurationResponse;

public interface InstanceConfigurationService {
    Map<InstanceConfigurationKey, String> configurations();

    List<InstanceConfigurationResponse> configurationsAll();

    List<InstanceConfigurationResponse> configurationsUpdate(Map<InstanceConfigurationKey, String> request);
}
