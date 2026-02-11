package com.syncturtle.platform.services.instance.payload;

import com.syncturtle.platform.services.instance.models.Instance;

public interface InstanceSummary {
    Instance getInstance();

    boolean isWorkspaceExist();

    long getUserCount();
}
