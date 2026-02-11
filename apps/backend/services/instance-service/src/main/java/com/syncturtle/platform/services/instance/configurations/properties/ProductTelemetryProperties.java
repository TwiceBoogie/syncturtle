package com.syncturtle.platform.services.instance.configurations.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.product-telemetry")
public class ProductTelemetryProperties {
    private final boolean enabled;
    private final String endpoint;
    private final String apiKey;
    private final Duration interval;
}
