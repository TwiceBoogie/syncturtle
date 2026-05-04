package com.syncturtle.services.user.payload;

import com.syncturtle.common.contracts.user.event.UserEvent;

public record UserEventToPublish(UserEvent event) {

}
