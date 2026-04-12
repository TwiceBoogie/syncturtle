package com.syncturtle.platform.services.instance.messaging;

import com.syncturtle.common.core.events.InstanceAdminSecurityEvent;

public interface InstanceAdminSecurityEventPublisher {
    void publishInstanceAdminSecurityEvent(InstanceAdminSecurityEvent event);
}
