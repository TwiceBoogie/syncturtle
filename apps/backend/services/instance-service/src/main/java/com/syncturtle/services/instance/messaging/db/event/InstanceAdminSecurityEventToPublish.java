package com.syncturtle.services.instance.messaging.db.event;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.event.InstanceAdminSecurityEvent;

import lombok.Getter;

@Getter
public final class InstanceAdminSecurityEventToPublish {
    private final InstanceAdminSecurityEvent event;

    public InstanceAdminSecurityEventToPublish(InstanceAdminSecurityEvent event) {
        Assert.notNull(event, "instance admin security event is required");
        this.event = event;
    }
}
