package com.syncturtle.services.user.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.endpoints")
public final class EndpointProperties {
    private final String webBaseUrl;
    private final String adminBaseUrl;
    private final String apiBaseUrl;

    public EndpointProperties(
            @DefaultValue("http://localhost:3000") String webBaseUrl,
            @DefaultValue("http://localhost:3001/god-mode") String adminBaseUrl,
            @DefaultValue("http://localhost:8000") String apiBaseUrl) {
        this.webBaseUrl = webBaseUrl;
        this.adminBaseUrl = adminBaseUrl;
        this.apiBaseUrl = apiBaseUrl;
    }
}
