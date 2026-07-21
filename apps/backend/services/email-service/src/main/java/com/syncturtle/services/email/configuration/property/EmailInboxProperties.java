package com.syncturtle.services.email.configuration.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.email.inbox", ignoreUnknownFields = false)
public final class EmailInboxProperties {

    private final ProcessingProperties processing;
    private final RetryProperties retry;

    public EmailInboxProperties(@DefaultValue ProcessingProperties processing, @DefaultValue RetryProperties retry) {
        Assert.notNull(processing, "app.email.inbox.processing is required");
        Assert.notNull(retry, "app.email.inbox.retry is required");

        this.processing = processing;
        this.retry = retry;
    }

    @Getter
    public static final class ProcessingProperties {

        private final Duration lease;
        private final int batchSize;

        public ProcessingProperties(
                @DefaultValue("5m") Duration lease,
                @DefaultValue("25") int batchSize) {
            this.lease = requirePositiveDuration(lease, "app.email.inbox.processing.lease");

            Assert.isTrue(batchSize >= 1, "app.email.inbox.processing.batch-size must be at least 1");

            this.batchSize = batchSize;
        }

    }

    @Getter
    public static final class RetryProperties {

        private final int maxAttempts;
        private final Duration initialDelay;
        private final Duration maxDelay;

        public RetryProperties(
                @DefaultValue("8") int maxAttempts,
                @DefaultValue("30s") Duration initialDelay,
                @DefaultValue("90s") Duration maxDelay) {
            Assert.isTrue(maxAttempts >= 1, "app.email.inbox.retry.max-attempts must be at least 1");
            Assert.isTrue(maxDelay.compareTo(initialDelay) >= 0,
                    "app.email.inbox.retry.max-delay must be greater than or equal to initial-delay");

            this.initialDelay = requirePositiveDuration(initialDelay, "app.email.inbox.retry.initial-delay");
            this.maxDelay = requirePositiveDuration(maxDelay, "app.email.inbox.retry.max-delay");
            this.maxAttempts = maxAttempts;
        }

    }

    private static Duration requirePositiveDuration(Duration duration, String propertyName) {
        Assert.notNull(duration, propertyName + " is required");
        Assert.isTrue(!duration.isNegative() && !duration.isZero(), propertyName + " must be positive");

        return duration;
    }

}
