package com.syncturtle.services.instance.support.fixture;

import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.services.instance.support.clock.TestClocks;

public final class InstanceEventFixtures {

    public static final String EVENT_ID = "instance-event-1001";
    public static final String API_BASE_URL = "http://localhost";

    private InstanceEventFixtures() {
        throw new AssertionError("InstanceEventFixtures must not be instantiated");
    }

    public static InstanceEvent instanceUpdate() {
        return InstanceEvent.builder()
                .eventId(EVENT_ID)
                .occurredAt(TestClocks.NOW)
                .type(InstanceEvent.Type.INSTANCE_UPDATED)
                .id(InstanceFixtures.INSTANCE_ID)
                .edition(InstanceFixtures.COMMUNITY_EDITION)
                .setupDone(true)
                .version(InstanceFixtures.VERSION)
                .apiBaseUrl(API_BASE_URL)
                .build();
    }

}
