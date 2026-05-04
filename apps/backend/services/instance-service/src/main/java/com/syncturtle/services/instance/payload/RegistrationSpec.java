package com.syncturtle.services.instance.payload;

import com.syncturtle.common.contracts.instance.model.InstanceEdition;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class RegistrationSpec {
    private final String instanceId;
    private final String instanceName;
    private final InstanceEdition edition;
    private final boolean telemetryEnabled;
    private final boolean supportRequired;
    private final boolean test;
    private final RuntimeMetadata runtime;
    private final BinaryMetadata binary;
}
