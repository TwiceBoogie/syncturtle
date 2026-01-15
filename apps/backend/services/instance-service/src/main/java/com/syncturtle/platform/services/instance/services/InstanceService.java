package com.syncturtle.platform.services.instance.services;

import java.util.List;
import java.util.Optional;

import com.syncturtle.platform.services.instance.models.User;
import com.syncturtle.platform.services.instance.repositories.InstanceInfoAggregate;
import com.syncturtle.platform.services.instance.repositories.projections.InstanceAdminProjection;

public interface InstanceService {
    Optional<InstanceInfoAggregate> instanceInfoAndConfig();

    Optional<User> getInstanceAdminUserMe();

    List<InstanceAdminProjection> getInstanceAdmins();
}
