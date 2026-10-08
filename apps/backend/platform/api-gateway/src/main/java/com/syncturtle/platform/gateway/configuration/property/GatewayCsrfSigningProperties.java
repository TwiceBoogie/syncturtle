package com.syncturtle.platform.gateway.configuration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.gateway.csrf.signing")
public final class GatewayCsrfSigningProperties {

    private final String key;

    public GatewayCsrfSigningProperties(String key) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("app.gateway.csrf.signing.key is required");
        }

        this.key = key.trim();
    }

}
