package com.syncturtle.platform.services.instance.payload;

import java.util.Map;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.models.Instance;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InstanceSummaryWithConfigResult implements InstanceSummaryWithConfig {
    private final Instance instance;
    private final Map<InstanceConfigurationKey, String> config;
    private final boolean workspacesExist;
    private final long userCount;

    @Override
    public Instance getInstance() {
        return instance;
    }

    @Override
    public boolean isWorkspaceExist() {
        return workspacesExist;
    }

    @Override
    public long getUserCount() {
        return userCount;
    }

    @Override
    public Map<InstanceConfigurationKey, String> getConfigurations() {
        return config;
    }
}
