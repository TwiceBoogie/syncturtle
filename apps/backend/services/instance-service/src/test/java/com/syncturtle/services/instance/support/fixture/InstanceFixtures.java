package com.syncturtle.services.instance.support.fixture;

import java.time.Instant;
import java.util.UUID;

import org.springframework.test.util.ReflectionTestUtils;

import com.syncturtle.common.contracts.instance.model.InstanceEdition;
import com.syncturtle.services.instance.model.Instance;
import com.syncturtle.services.instance.model.param.InstanceBinaryParam;
import com.syncturtle.services.instance.model.param.InstanceRegistrationFlagsParam;
import com.syncturtle.services.instance.model.param.InstanceRegistrationParam;
import com.syncturtle.services.instance.model.param.InstanceRuntimeParam;

public final class InstanceFixtures {

    public static final Instant REGISTERED_AT = Instant.parse("2026-05-23T13:00:00Z");
    public static final Instant UPDATED_AT = Instant.parse("2026-05-23T13:30:00Z");
    public static final UUID INSTANCE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final String MACHINE_SIG = "instance-001";
    public static final InstanceEdition COMMUNITY_EDITION = InstanceEdition.COMMUNITY;
    public static final Long GLOBAL_CONFIG_VERSION = 10L;
    public static final Long VERSION = 7L;

    private InstanceFixtures() {
    }

    public static Instance activeInstance() {
        return activeInstance("Syncturtle");
    }

    public static Instance activeInstance(String name) {
        return Instance.register(registrationParam(name));
    }

    public static Instance persistedInstance(String name) {
        Instance instance = activeInstance(name);
        markPersisted(instance, INSTANCE_ID);
        return instance;
    }

    public static Instance deletedInstance(String name) {
        Instance instance = persistedInstance(name);
        ReflectionTestUtils.setField(instance, "deletedAt", Instant.parse("2026-05-23T14:15:00Z"));
        return instance;
    }

    public static InstanceRegistrationParam registrationParam(String name) {
        return InstanceRegistrationParam.builder()
                .instanceId(MACHINE_SIG)
                .instanceName(name)
                .edition(COMMUNITY_EDITION)
                .flags(InstanceRegistrationFlagsParam.builder()
                        .telemetryEnabled(true)
                        .supportRequired(false)
                        .test(true)
                        .build())
                .runtime(InstanceRuntimeParam.builder()
                        .domain("syncturtle.local")
                        .namespace("dev")
                        .vmHost("localhost")
                        .build())
                .binary(InstanceBinaryParam.of("0.0.1-test", REGISTERED_AT))
                .registeredAt(REGISTERED_AT)
                .build();
    }

    public static void markPersisted(Instance instance, UUID id) {
        ReflectionTestUtils.setField(instance, "id", id);
        ReflectionTestUtils.setField(instance, "version", 7L);
        ReflectionTestUtils.setField(instance, "createdAt", REGISTERED_AT);
        ReflectionTestUtils.setField(instance, "updatedAt", UPDATED_AT);
        ReflectionTestUtils.setField(instance, "createdById", UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
        ReflectionTestUtils.setField(instance, "updatedById", UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
    }

}
