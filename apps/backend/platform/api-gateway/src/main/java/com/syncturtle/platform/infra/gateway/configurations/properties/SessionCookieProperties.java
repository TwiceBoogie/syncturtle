package com.syncturtle.platform.infra.gateway.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.gateway.session.cookie")
public final class SessionCookieProperties {
    private final boolean httpOnly;
    private final boolean secure;
    private final String sameSite;
    private final String path;
    private final String domain;
}
