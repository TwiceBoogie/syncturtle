package com.syncturtle.platform.services.instance.payload;

import com.syncturtle.common.core.events.InstanceEvent;

/**
 * small app event wrapper
 */
public record InstanceEventToPublish(InstanceEvent event) {

}
