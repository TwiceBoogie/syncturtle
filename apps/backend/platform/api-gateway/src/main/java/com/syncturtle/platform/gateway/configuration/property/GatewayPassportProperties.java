package com.syncturtle.platform.gateway.configuration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.passport.gateway", ignoreUnknownFields = false)
public final class GatewayPassportProperties {

    private final String issuer;
    private final String audience;

    public GatewayPassportProperties(
            String issuer,
            String audience) {
        this.issuer = requireText(issuer, "issuer");
        this.audience = requireText(audience, "audience");
    }

    private static String requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(
                    "app.passport.gateway." + propertyName + " is required");
        }

        return value.trim();
    }

}
