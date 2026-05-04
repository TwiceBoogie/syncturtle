package com.syncturtle.services.user.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.passport")
public class PassportProperties {
    private final String issuer;
    private final String audience;
    private final String kid;
    private final String privateKeyLocation;
    private final String publicKeyLocation;

    public PassportProperties(
            @DefaultValue("https://api.syncturtle.com/auth") String issuer,
            @DefaultValue("syncturtle-api") String audience,
            @DefaultValue("auth-key-2026-04") String kid,
            @DefaultValue("classpath:keys/jwt-private-key.pem") String privateKeyLocation,
            @DefaultValue("classpath:keys/jwt-public-key.pem") String publicKeyLocation) {
        this.issuer = issuer;
        this.audience = audience;
        this.kid = kid;
        this.privateKeyLocation = privateKeyLocation;
        this.publicKeyLocation = publicKeyLocation;
    }

    public boolean hasKid() {
        return StringUtils.hasText(kid);
    }
}
