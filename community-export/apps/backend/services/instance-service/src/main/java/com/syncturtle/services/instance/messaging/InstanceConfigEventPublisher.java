package com.syncturtle.services.instance.messaging;

import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;

public interface InstanceConfigEventPublisher {
    void publishInstanceConfigurationEvent(InstanceConfigurationEvent event);
}
