package com.syncturtle.services.instance.service.param;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

import lombok.Builder;
import lombok.Getter;

@Getter
public final class DerivedFlagParam {

    private final InstanceConfigurationKey key;
    private final boolean enabled;

    @Builder
    private DerivedFlagParam(InstanceConfigurationKey key, Boolean enabled) {
        Assert.notNull(key, "key is required");
        Assert.notNull(enabled, "enabled is requied");

        this.key = key;
        this.enabled = enabled;
    }

}
