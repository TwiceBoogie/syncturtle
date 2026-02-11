package com.syncturtle.platform.services.instance.payload;

import java.util.Map;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;

public interface InstanceSummaryWithConfig extends InstanceSummary {
    Map<InstanceConfigurationKey, String> getConfigurations();
}
