package com.syncturtle.services.file.configuration.property;

import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.Assert;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.file.cleanup")
public final class FileCleanupProperties {

    private final boolean enabled;
    private final long fixedDelayMs;
    private final long initialDelayMs;
    private final int batchSize;
    private final Duration claimLease;
    private final Duration unlinkedAssetRetention;

    public FileCleanupProperties(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("60000") long fixedDelayMs,
            @DefaultValue("10000") long initialDelayMs,
            @DefaultValue("50") int batchSize,
            @DefaultValue("5m") Duration claimLease,
            @DefaultValue("7d") Duration unlinkedAssetRetention) {
        Assert.isTrue(fixedDelayMs >= 1_000, "app.file.cleanup.fixedDelayMs must be at least 1000");
        Assert.isTrue(initialDelayMs >= 0, "app.file.cleanup.initialDelayMs must not be negative");
        Assert.isTrue(batchSize >= 1 && batchSize <= 1_000,
                "app.file.cleanup.batchSize must be between 1 and 1000");

        Duration normalizedLease = Objects.requireNonNull(claimLease, "app.file.cleanup.claimLease is required");
        Assert.isTrue(!normalizedLease.isZero() && !normalizedLease.isNegative(),
                "app.file.cleanup.claimLease must be positive");
        Duration normalizedUnlinkedRetention = Objects.requireNonNull(unlinkedAssetRetention,
                "app.file.cleanup.unlinkedAssetRetention is required");
        Assert.isTrue(!normalizedUnlinkedRetention.isZero() && !normalizedUnlinkedRetention.isNegative(),
                "app.file.cleanup.unlinkedAssetRetention must be positive");

        this.enabled = enabled;
        this.fixedDelayMs = fixedDelayMs;
        this.initialDelayMs = initialDelayMs;
        this.batchSize = batchSize;
        this.claimLease = normalizedLease;
        this.unlinkedAssetRetention = normalizedUnlinkedRetention;
    }

}
