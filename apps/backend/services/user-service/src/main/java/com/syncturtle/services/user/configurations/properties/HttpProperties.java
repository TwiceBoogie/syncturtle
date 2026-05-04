package com.syncturtle.services.user.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.ws.rs.DefaultValue;
import lombok.Getter;

/**
 * HTTP-related configurations for the application
 */
@Getter
@ConfigurationProperties(prefix = "app.http")
public final class HttpProperties {

    /**
     * Explicit cookie domain for cookies when sharing across subdomains
     */
    private final String cookieDomain;

    public HttpProperties(
            @DefaultValue("") String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }
}
