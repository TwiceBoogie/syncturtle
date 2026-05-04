package com.syncturtle.services.instance.messaging;

import com.syncturtle.common.contracts.instance.event.InstanceEvent;

public interface InstanceEventPublisher {
    void publishInstanceEvent(InstanceEvent event);
}
