package com.syncturtle.platform.infra.gateway.configurations.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.gateway.session")
public final class SessionGatewayProperties {
    private final String redisKeyPrefix;
    private final Duration sessionTtl;
    private final Duration absoluteTtl;
    // renew when remaining < 25%
    private final double renewWhenRemainingFraction = 0.25;
}
