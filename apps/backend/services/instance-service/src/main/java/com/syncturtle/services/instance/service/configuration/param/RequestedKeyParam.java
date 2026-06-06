package com.syncturtle.services.instance.service.configuration.param;

import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.config.InstanceConfigurationKey;

import lombok.Getter;

@Getter
public final class RequestedKeyParam {

    private final InstanceConfigurationKey key;
    private final String defaultValue;

    private RequestedKeyParam(InstanceConfigurationKey key, String defaultValue) {
        Assert.notNull(key, "configuration key is required");

        this.key = key;
        this.defaultValue = defaultValue;
    }

    public static RequestedKeyParam of(InstanceConfigurationKey key) {
        return new RequestedKeyParam(key, null);
    }

    public static RequestedKeyParam of(InstanceConfigurationKey key, String defaultValue) {
        return new RequestedKeyParam(key, defaultValue);
    }

    public boolean hasDefaultOverride() {
        return defaultValue != null;
    }

}
