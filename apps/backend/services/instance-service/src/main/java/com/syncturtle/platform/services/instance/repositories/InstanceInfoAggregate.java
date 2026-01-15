package com.syncturtle.platform.services.instance.repositories;

import java.util.Map;

import com.syncturtle.common.core.enums.InstanceConfigurationKey;
import com.syncturtle.platform.services.instance.models.readmodel.InstanceInfoRow;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstanceInfoAggregate {
    private final InstanceInfoRow instance;
    private final Map<InstanceConfigurationKey, String> config;
    private final boolean workspacesExist;
    private final long userCount;
}
