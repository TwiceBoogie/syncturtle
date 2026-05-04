package com.syncturtle.services.instance.dto.result;

import java.util.Map;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstanceConfigResult {
    Map<InstanceConfigurationKey, String> values;
    Long version;
}
