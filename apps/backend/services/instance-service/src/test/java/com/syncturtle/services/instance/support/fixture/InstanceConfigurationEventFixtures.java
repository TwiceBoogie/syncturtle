package com.syncturtle.services.instance.support.fixture;

import java.util.Set;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.common.contracts.instance.config.InstanceConfigurationScopeNames;
import com.syncturtle.common.contracts.instance.event.InstanceConfigurationEvent;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.support.clock.TestClocks;

public final class InstanceConfigurationEventFixtures {

    public static final String EVENT_ID = "instance-configuration-event-1001";

    private InstanceConfigurationEventFixtures() {
        throw new AssertionError("InstanceConfigurationEventFixtures must not be instantiated");
    }

    public InstanceConfigurationEvent event(Instance instance, Set<InstanceConfigurationKey> changedKeys,
            InstanceConfigurationScopeNames scope) {
        return InstanceConfigurationEvent.builder()
                .eventId(EVENT_ID)
                .correlationId(instance.getId().toString())
                .occurredAt(TestClocks.NOW)
                .instanceId(InstanceFixtures.INSTANCE_ID)
                .scope(scope)
                .changedKeys(changedKeys)
                .scopeVersion(InstanceFixtures.GLOBAL_CONFIG_VERSION)
                .globalVersion(InstanceFixtures.GLOBAL_CONFIG_VERSION)
                .build();
    }

}
