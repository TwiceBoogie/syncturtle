package com.syncturtle.services.instance.services;

import java.util.Optional;

import com.syncturtle.services.instance.dto.request.InstanceRequest;
import com.syncturtle.services.instance.models.User;
import com.syncturtle.services.instance.payload.InstanceSummary;
import com.syncturtle.services.instance.payload.InstanceSummaryWithConfig;

public interface InstanceService {
    Optional<InstanceSummaryWithConfig> instanceInfoAndConfig();

    Optional<User> getInstanceAdminUserMe();

    InstanceSummary instanceUpdate(InstanceRequest request);
}
