package com.syncturtle.services.instance.payload;

import com.syncturtle.services.instance.models.Instance;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class InstanceSummaryResult implements InstanceSummary {
    private final Instance instance;
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
}
