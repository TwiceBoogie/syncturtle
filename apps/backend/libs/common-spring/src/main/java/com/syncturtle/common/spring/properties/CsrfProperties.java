package com.syncturtle.common.spring.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.security.csrf")
public class CsrfProperties {
    private final String hmacAlgorithm;
    private final String signingKey;
}
