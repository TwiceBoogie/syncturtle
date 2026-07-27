package com.syncturtle.services.instance.messaging.outbox;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class OutboxRetryDelayPolicy {

    private static final Duration FIRST_RETRY_DELAY = Duration.ofSeconds(1);
    private static final Duration SECOND_RETRY_DELAY = Duration.ofSeconds(5);
    private static final Duration THIRD_RETRY_DELAY = Duration.ofSeconds(15);
    private static final Duration FOURTH_RETRY_DELAY = Duration.ofMinutes(1);
    private static final Duration LATER_RETRY_DELAY = Duration.ofMinutes(5);

    public Duration nextDelay(OutboxEnvelope envelope) {
        Assert.notNull(envelope, "outbox envelope is required");

        int nextAttempt = envelope.getAttempts() + 1;

        if (nextAttempt <= 1) {
            return FIRST_RETRY_DELAY;
        }

        if (nextAttempt == 2) {
            return SECOND_RETRY_DELAY;
        }

        if (nextAttempt == 3) {
            return THIRD_RETRY_DELAY;
        }

        if (nextAttempt == 4) {
            return FOURTH_RETRY_DELAY;
        }

        return LATER_RETRY_DELAY;
    }

}
