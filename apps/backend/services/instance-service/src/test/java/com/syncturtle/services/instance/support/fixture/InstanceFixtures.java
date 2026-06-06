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
        markPersisted(instance, UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        return instance;
    }

    public static Instance deletedInstance(String name) {
        Instance instance = persistedInstance(name);
        ReflectionTestUtils.setField(instance, "deletedAt", Instant.parse("2026-05-23T14:15:00Z"));
        return instance;
    }

    public static InstanceRegistrationParam registrationParam(String name) {
        return InstanceRegistrationParam.builder()
                .instanceId(" instance-001 ")
                .instanceName(name)
                .edition(firstEdition())
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

    private static InstanceEdition firstEdition() {
        return InstanceEdition.values()[0];
    }

}
