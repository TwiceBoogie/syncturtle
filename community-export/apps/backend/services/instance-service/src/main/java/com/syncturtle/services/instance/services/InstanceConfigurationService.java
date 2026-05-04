package com.syncturtle.services.instance.services;

import java.util.List;
import java.util.Map;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.dto.response.InstanceConfigurationResponse;
import com.syncturtle.services.instance.dto.result.InstanceConfigResult;

public interface InstanceConfigurationService {
    InstanceConfigResult configurations();

    List<InstanceConfigurationResponse> configurationsAll();

    List<InstanceConfigurationResponse> configurationsUpdate(Map<InstanceConfigurationKey, String> request);

    void disableEmail();
}
