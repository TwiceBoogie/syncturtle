package com.syncturtle.services.instance.payload;

import com.syncturtle.common.contracts.instance.event.InstanceEvent;

/**
 * small app event wrapper
 */
public record InstanceEventToPublish(InstanceEvent event) {

}
