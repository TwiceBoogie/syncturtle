package com.syncturtle.common.spring.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.security.csrf.transport")
public final class CsrfTransportProperties {
    private final String cookieName;
    private final String headerName;
    private final String formFieldName;
}
