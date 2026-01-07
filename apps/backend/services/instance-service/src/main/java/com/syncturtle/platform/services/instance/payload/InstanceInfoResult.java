package com.syncturtle.platform.services.instance.payload;

import java.util.Map;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.repositories.projections.InstanceProjection;

import lombok.Data;

@Data
public class InstanceInfoResult {
    private InstanceProjection instance;
    private Map<InstanceConfigurationKey, String> config;
}
