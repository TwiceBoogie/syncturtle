package com.syncturtle.services.user.configuration.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.passport", ignoreUnknownFields = false)
public final class PassportProperties {

    private final String issuer;
    private final String audience;
    private final String kid;
    private final String privateKeyLocation;
    private final String publicKeyLocation;

    public PassportProperties(
            String issuer,
            String audience,
            String kid,
            String privateKeyLocation,
            String publicKeyLocation) {
        this.issuer = requireText(issuer, "issuer");
        this.audience = requireText(audience, "audience");
        this.kid = requireText(kid, "kid");
        this.privateKeyLocation = requireText(privateKeyLocation, "private-key-location");
        this.publicKeyLocation = requireText(publicKeyLocation, "public-key-location");
    }

    public boolean hasKid() {
        return StringUtils.hasText(kid);
    }

    private static String requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("app.passport." + propertyName + " is required");
        }

        return value.trim();
    }

}
