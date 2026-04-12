package com.syncturtle.platform.services.instance.messaging.db;

import com.syncturtle.common.core.events.InstanceConfigurationEvent;

public record InstanceConfigurationEventToPublish(InstanceConfigurationEvent event) {
}
