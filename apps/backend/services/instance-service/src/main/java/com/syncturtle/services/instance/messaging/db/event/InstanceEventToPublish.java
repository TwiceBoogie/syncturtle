package com.syncturtle.services.instance.messaging.db.event;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.event.InstanceEvent;

import lombok.Getter;

/**
 * small app event wrapper
 */
@Getter
public final class InstanceEventToPublish {
    private final InstanceEvent event;

    public InstanceEventToPublish(InstanceEvent event) {
        Assert.notNull(event, "instance event is required");
        this.event = event;
    }
}