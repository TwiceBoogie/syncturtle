package com.syncturtle.platform.services.instance.events;

import java.util.UUID;

public class InstanceRegisteredEvent {
    private final UUID instanceId;

    public InstanceRegisteredEvent(UUID instanceId) {
        this.instanceId = instanceId;
    }

    public UUID getInstanceId() {
        return instanceId;
    }
}
