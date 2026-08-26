package com.syncturtle.services.email.service;

import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;

public interface EmailRuntimeConfigChangeService {
    void receive(InstanceConfigurationEvent event);
}
