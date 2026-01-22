package com.syncturtle.platform.services.instance.messaging;

import com.syncturtle.common.core.events.InstanceEvent;

public interface InstanceEventPublisher {
    void publishInstanceEvent(InstanceEvent event);
}
