package com.syncturtle.services.instance.messaging.db;

import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;

public record InstanceConfigurationEventToPublish(InstanceConfigurationEvent event) {
}
