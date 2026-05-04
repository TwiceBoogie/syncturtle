package com.syncturtle.services.instance.payload;

import java.util.Map;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

public interface InstanceSummaryWithConfig extends InstanceSummary {
    Map<InstanceConfigurationKey, String> getConfigurations();
}
