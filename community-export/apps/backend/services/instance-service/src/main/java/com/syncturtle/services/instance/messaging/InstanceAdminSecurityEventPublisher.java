package com.syncturtle.services.instance.messaging;

import com.syncturtle.common.contracts.instance.event.InstanceAdminSecurityEvent;

public interface InstanceAdminSecurityEventPublisher {
    void publishInstanceAdminSecurityEvent(InstanceAdminSecurityEvent event);
}
