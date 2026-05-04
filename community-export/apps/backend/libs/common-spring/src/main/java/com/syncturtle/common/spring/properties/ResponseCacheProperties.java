package com.syncturtle.common.spring.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.response-cache")
public final class ResponseCacheProperties {
    private final boolean enabled = true;
    private final String keyPrefix = "st:resp:";
    private final int hashBytes = 16;
}
