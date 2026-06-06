package com.syncturtle.common.spring.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.gateway.context")
public final class GatewayContextProperties {

    private final boolean enabled;

    public GatewayContextProperties(@DefaultValue("true") boolean enabled) {
        this.enabled = enabled;
    }

}
