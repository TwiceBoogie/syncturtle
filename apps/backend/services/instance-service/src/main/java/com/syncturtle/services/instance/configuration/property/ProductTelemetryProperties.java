package com.syncturtle.services.instance.configuration.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.product-telemetry", ignoreUnknownFields = false)
public final class ProductTelemetryProperties {

    private final boolean enabled;
    private final String endpoint;
    private final String apiKey;
    private final Duration interval;

    public ProductTelemetryProperties(boolean enabled, String endpoint, String apiKey, Duration interval) {
        if (enabled && !StringUtils.hasText(endpoint)) {
            throw new IllegalArgumentException(
                    "app.product-telemetry.endpoint is required when product telemetry is enabled");
        }

        if (interval == null) {
            throw new IllegalArgumentException("app.product-telemetry.interval is required");
        }

        if (interval.isZero() || interval.isNegative()) {
            throw new IllegalArgumentException("app.product-telemetry.interval must be positive");
        }

        this.enabled = enabled;
        this.endpoint = StringUtils.hasText(endpoint) ? endpoint.trim() : "";
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.interval = interval;
    }

}
