package com.syncturtle.common.spring.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.security.csrf.cookie-policy")
public final class CsrfCookiePolicyProperties {
    private final boolean httpOnly;
    private final boolean secure;
    private final String sameSite;
    private final String path;
    private final String domain;
    private final Duration maxAge;
}
