package com.syncturtle.services.instance.payload;

import com.syncturtle.services.instance.models.Instance;

public interface InstanceSummary {
    Instance getInstance();

    boolean isWorkspaceExist();

    long getUserCount();
}
