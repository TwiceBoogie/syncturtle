package com.syncturtle.services.instance.payload;

import java.util.Map;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.repositories.projections.InstanceProjection;

import lombok.Data;

@Data
public class InstanceInfoResult {
    private InstanceProjection instance;
    private Map<InstanceConfigurationKey, String> config;
}
