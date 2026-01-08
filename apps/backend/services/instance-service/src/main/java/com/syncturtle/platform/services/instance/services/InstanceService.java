package com.syncturtle.platform.services.instance.services;

import java.util.Optional;

import com.syncturtle.platform.services.instance.repositories.InstanceInfoAggregate;

public interface InstanceService {
    Optional<InstanceInfoAggregate> instanceInfoAndConfig();
}
