package com.syncturtle.platform.gateway.security;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.syncturtle.platform.gateway.type.PassportAuthenticationFailureReason;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class PassportAuthenticationMetrics {

    private static final String METRIC_NAME = "syncturtle.gateway.passport.authentication";

    private final Counter accepted;
    private final Map<PassportAuthenticationFailureReason, Counter> rejected;

    public PassportAuthenticationMetrics(MeterRegistry meterRegistry) {
        this.accepted = meterRegistry.counter(METRIC_NAME, "outcome", "accepted", "reason", "none");
        this.rejected = new EnumMap<>(PassportAuthenticationFailureReason.class);

        for (PassportAuthenticationFailureReason reason : PassportAuthenticationFailureReason.values()) {
            Counter counter = meterRegistry.counter(
                    METRIC_NAME,
                    "outcome",
                    "rejected",
                    "reason",
                    reason.metricValue());
            rejected.put(reason, counter);
        }
    }

    public void accepted() {
        accepted.increment();
    }

    public void rejected(PassportAuthenticationFailureReason reason) {
        Counter counter = rejected.get(reason);
        if (counter != null) {
            counter.increment();
        }
    }

}
