package com.syncturtle.services.instance.model.embedded;

import java.time.Clock;
import java.time.Instant;

import org.springframework.util.Assert;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfigInfoEmbed {

    private static final Long INITIAL_VERSION = 0L;

    @Column(name = "config_version", nullable = false)
    private Long version;

    @Column(name = "config_last_checked_at")
    private Instant lastCheckedAt;

    public void initialize(Instant now) {
        Assert.notNull(now, "now is required");

        version = INITIAL_VERSION;
        lastCheckedAt = now;
    }

    public void initializeIfMissing(Instant now) {
        Assert.notNull(now, "now is required");

        if (version != null) {
            return;
        }

        initialize(now);
    }

    public void bumpVersion(Clock clock) {
        Assert.notNull(clock, "clock is required");
        Assert.state(version != null, "config version must be initialized before it can be bumped");

        if (version == Long.MAX_VALUE) {
            throw new IllegalStateException("config version overflow");
        }

        version++;
        lastCheckedAt = Instant.now(clock);
    }

    public static ConfigInfoEmbed empty() {
        return new ConfigInfoEmbed();
    }

}
