package com.syncturtle.common.web.properties;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.kafka.error-handling")
public class KafkaErrorHandlingProperties {

    private final Duration initialInterval;
    private final double multiplier;
    private final Duration maxElapsedTime;
    private final String dltSuffix;
    private final boolean ackAfterHandle;
    private final boolean commitRecovered;
    private final boolean logRetries;

    public KafkaErrorHandlingProperties(
            @DefaultValue("500ms") Duration initialInterval,
            @DefaultValue("2.0") double multiplier,
            @DefaultValue("8s") Duration maxElapsedTime,
            @DefaultValue(".DLT") String dltSuffix,
            @DefaultValue("true") boolean ackAfterHandle,
            @DefaultValue("true") boolean commitRecovered,
            @DefaultValue("true") boolean logRetries) {
        this.initialInterval = requirePositive(initialInterval, "initial-interval");
        this.multiplier = requireAtLeast(multiplier, 1.0d, "multiplier");
        this.maxElapsedTime = requirePositive(maxElapsedTime, "max-elapsed-time");
        this.dltSuffix = requireText(dltSuffix, "dlt-suffix");
        this.ackAfterHandle = ackAfterHandle;
        this.commitRecovered = commitRecovered;
        this.logRetries = logRetries;
    }

    private static Duration requirePositive(Duration value, String propertyName) {
        Duration duration = Objects.requireNonNull(value, "app.kafka.error-handling." + propertyName + " is required");

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("app.kafka.error-handling." + propertyName + " must be positive");
        }

        return duration;
    }

    private static double requireAtLeast(double value, double min, String propertyName) {
        if (value < min) {
            throw new IllegalArgumentException(
                    "app.kafka.error-handling." + propertyName + " must be >= " + min);
        }

        return value;
    }

    private static String requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("app.kafka.error-handling." + propertyName + " is required");
        }

        return value.trim();
    }

}
