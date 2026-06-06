package com.syncturtle.services.instance.model.param;

import java.time.Instant;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.model.InstanceEdition;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class InstanceRegistrationParam {

    private static final int MAX_INSTANCE_NAME_LENGTH = 100;

    private final String instanceId;
    private final String instanceName;
    private final InstanceEdition edition;
    private final InstanceRegistrationFlagsParam flags;
    private final InstanceRuntimeParam runtime;
    private final InstanceBinaryParam binary;
    private final Instant registeredAt;

    @Builder
    private InstanceRegistrationParam(
            String instanceId,
            String instanceName,
            InstanceEdition edition,
            InstanceRegistrationFlagsParam flags,
            InstanceRuntimeParam runtime,
            InstanceBinaryParam binary,
            Instant registeredAt) {
        Assert.hasText(instanceId, "instanceId is required");
        Assert.notNull(edition, "edition is required");
        Assert.notNull(flags, "registration flags are required");
        Assert.notNull(runtime, "runtime is required");
        Assert.notNull(binary, "binary is required");
        Assert.notNull(registeredAt, "registeredAt is required");

        this.instanceId = instanceId.trim();
        this.instanceName = normalizeOptionalName(instanceName);
        this.edition = edition;
        this.flags = flags;
        this.runtime = runtime;
        this.binary = binary;
        this.registeredAt = registeredAt;
    }

    private static String normalizeOptionalName(String value) {
        String normalized = normalizeNullable(value);

        if (normalized != null) {
            Assert.isTrue(normalized.length() <= MAX_INSTANCE_NAME_LENGTH,
                    "instanceName must be " + MAX_INSTANCE_NAME_LENGTH + " characters or fewer");
        }

        return normalized;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
