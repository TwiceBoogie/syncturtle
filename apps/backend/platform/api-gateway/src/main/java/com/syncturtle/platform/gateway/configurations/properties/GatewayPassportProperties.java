package com.syncturtle.platform.gateway.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.passport.gateway")
public class GatewayPassportProperties {
    private String issuer;
    private String audience;
    private String accessCookieName;
    private String authHeaderPrefix;
    private Redis redis;

    @Getter
    @Setter
    public class Redis {
        private String sessionKeyPrefix;
        private String userVersionKeyPrefix;
    }
}
