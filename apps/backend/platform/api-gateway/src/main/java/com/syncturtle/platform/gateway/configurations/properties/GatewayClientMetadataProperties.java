package com.syncturtle.platform.gateway.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.gateway.client-metadata", ignoreUnknownFields = false)
public final class GatewayClientMetadataProperties {

    private final int trustedProxyCount;

    public GatewayClientMetadataProperties(@DefaultValue("0") int trustedProxyCount) {
        if (trustedProxyCount < 0) {
            throw new IllegalArgumentException("app.gateway.client-metadata.trusted-proxy-count must not be negative");
        }

        this.trustedProxyCount = trustedProxyCount;
    }

}
