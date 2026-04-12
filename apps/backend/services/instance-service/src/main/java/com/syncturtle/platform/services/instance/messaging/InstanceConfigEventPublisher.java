package com.syncturtle.platform.services.instance.messaging;

import com.syncturtle.common.core.events.InstanceConfigurationEvent;

public interface InstanceConfigEventPublisher {
    void publishInstanceConfigurationEvent(InstanceConfigurationEvent event);
}
