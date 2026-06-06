package com.syncturtle.services.instance.repository;

import java.util.Map;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;
import com.syncturtle.services.instance.model.Instance;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class InstanceInfoAggregate {
    private final Instance instance;
    private final Map<InstanceConfigurationKey, String> config;
    private final boolean workspacesExist;
    private final long userCount;
}
