package com.syncturtle.services.email.configuration.property;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.Assert;

import lombok.Getter;

/**
 * Lease duration, retry policy, and retry batch configuration.
 */
@Getter
@ConfigurationProperties(prefix = "app.email.inbox", ignoreUnknownFields = false)
public final class EmailInboxProperties {

    /**
     * Settings that control how rows are claimed for active processing.
     *
     * <p>
     * Configuration prefix: {@code app.email.inbox.processing}.
     */
    private final ProcessingProperties processing;

    /**
     * Settings that control retry limits and exponential backoff.
     *
     * <p>
     * Configuration prefix: {@code app.email.inbox.retry}.
     */
    private final RetryProperties retry;

    /**
     * Scheduler activation and cadence. These values are operational and must be
     * visible in the Spring Environment, so they are supplied by configuration
     * data rather than Java defaults.
     */
    private final SchedulerProperties scheduler;

    public EmailInboxProperties(
            @DefaultValue ProcessingProperties processing,
            @DefaultValue RetryProperties retry,
            SchedulerProperties scheduler) {
        Assert.notNull(processing, "app.email.inbox.processing is required");
        Assert.notNull(retry, "app.email.inbox.retry is required");
        Assert.notNull(scheduler, "app.email.inbox.scheduler is required");

        this.processing = processing;
        this.retry = retry;
        this.scheduler = scheduler;
    }

    /**
     * Ownership settings used when claiming new, retryable, or abandoned inbox
     * rows.
     */
    @Getter
    public static final class ProcessingProperties {

        /**
         * Length of time one service instance owns a row after claiming it
         * 
         * *
         * <p>
         * Configuration property: {@code app.email.inbox.processing.lease}.
         * Default: {@code 5m}.
         */
        private final Duration lease;

        /**
         * Maximum number of due retry and expired lease rows claimbed by one scheduler
         * 
         * *
         * <p>
         * Configuration property: {@code app.email.inbox.processing.batch-size}.
         * Default: {@code 25}.
         */
        private final int batchSize;

        public ProcessingProperties(
                @DefaultValue("5m") Duration lease,
                @DefaultValue("25") int batchSize) {
            this.lease = requirePositiveDuration(lease, "app.email.inbox.processing.lease");

            Assert.isTrue(batchSize >= 1, "app.email.inbox.processing.batch-size must be at least 1");

            this.batchSize = batchSize;
        }

    }

    /**
     * Retry policy applied after retryable delivery failures.
     */
    @Getter
    public static final class RetryProperties {

        /**
         * Maximum total number of delivery attempts, including the initial attempt.
         * 
         * <p>
         * Configuration property: {@code app.email.inbox.retry.max-attempts}.
         * Default: {@code 8}.
         */
        private final int maxAttempts;

        /**
         * Delay scheduled after the first failed delivery attempt.
         *
         * <p>
         * Configuration property: {@code app.email.inbox.retry.initial-delay}.
         * Default: {@code 30s}.
         */
        private final Duration initialDelay;

        /**
         * Upper bound for exponential retry delay.
         *
         * <p>
         * Configuration property: {@code app.email.inbox.retry.max-delay}.
         * Default: {@code 90s}.
         */
        private final Duration maxDelay;

        public RetryProperties(
                @DefaultValue("8") int maxAttempts,
                @DefaultValue("30s") Duration initialDelay,
                @DefaultValue("90s") Duration maxDelay) {
            Assert.isTrue(maxAttempts >= 1, "app.email.inbox.retry.max-attempts must be at least 1");
            this.initialDelay = requirePositiveDuration(initialDelay, "app.email.inbox.retry.initial-delay");
            this.maxDelay = requirePositiveDuration(maxDelay, "app.email.inbox.retry.max-delay");
            Assert.isTrue(maxDelay.compareTo(initialDelay) >= 0,
                    "app.email.inbox.retry.max-delay must be greater than or equal to initial-delay");

            this.maxAttempts = maxAttempts;
        }

    }

    @Getter
    public static final class SchedulerProperties {

        private final boolean enabled;
        private final Duration pollDelay;

        public SchedulerProperties(boolean enabled, Duration pollDelay) {
            this.enabled = enabled;
            this.pollDelay = requirePositiveDuration(
                    pollDelay,
                    "app.email.inbox.scheduler.poll-delay");
        }
    }

    private static Duration requirePositiveDuration(Duration duration, String propertyName) {
        Assert.notNull(duration, propertyName + " is required");
        Assert.isTrue(!duration.isNegative() && !duration.isZero(), propertyName + " must be positive");

        return duration;
    }

}
