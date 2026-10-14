package com.syncturtle.common.kafka.property;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.kafka.error-handling")
public final class KafkaErrorHandlingProperties {

    private static final Duration MAX_INITIAL_INTERVAL = Duration.ofSeconds(5);
    private static final Duration MAX_ELAPSED_TIME = Duration.ofMinutes(2);
    private static final double MAX_MULTIPLIER = 4.0d;

    private final Duration initialInterval;
    private final double multiplier;
    private final Duration maxElapsedTime;
    private final boolean logRetries;

    public KafkaErrorHandlingProperties(
            @DefaultValue("500ms") Duration initialInterval,
            @DefaultValue("2.0") double multiplier,
            @DefaultValue("8s") Duration maxElapsedTime,
            @DefaultValue("true") boolean logRetries) {
        this.initialInterval = requirePositive(initialInterval, "initial-interval");
        if (this.initialInterval.compareTo(MAX_INITIAL_INTERVAL) > 0) {
            throw new IllegalArgumentException("app.kafka.error-handling.initial-interval must be at most 5s");
        }
        this.multiplier = requireMultiplier(multiplier);
        this.maxElapsedTime = requirePositive(maxElapsedTime, "max-elapsed-time");
        if (this.maxElapsedTime.compareTo(this.initialInterval) < 0
                || this.maxElapsedTime.compareTo(MAX_ELAPSED_TIME) > 0) {
            throw new IllegalArgumentException(
                    "app.kafka.error-handling.max-elapsed-time must be between initial-interval and 2m");
        }
        this.logRetries = logRetries;
    }

    private static Duration requirePositive(Duration value, String propertyName) {
        Duration duration = Objects.requireNonNull(value, "app.kafka.error-handling." + propertyName + " is required");

        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("app.kafka.error-handling." + propertyName + " must be positive");
        }

        return duration;
    }

    private static double requireMultiplier(double value) {
        if (!Double.isFinite(value) || value < 1.0d || value > MAX_MULTIPLIER) {
            throw new IllegalArgumentException("app.kafka.error-handling.multiplier must be between 1 and 4");
        }

        return value;
    }

}
