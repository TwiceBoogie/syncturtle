package com.syncturtle.services.instance.messaging.db.event;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;

import lombok.Getter;

@Getter
public final class InstanceConfigurationEventToPublish {
    private final InstanceConfigurationEvent event;

    public InstanceConfigurationEventToPublish(InstanceConfigurationEvent event) {
        Assert.notNull(event, "instance configuration event is required");
        this.event = event;
    }
}