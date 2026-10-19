package com.syncturtle.common.cache.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.response-cache")
public final class ResponseCacheProperties {

    private final boolean enabled;

    public ResponseCacheProperties(@DefaultValue("true") boolean enabled) {
        this.enabled = enabled;
    }

}
