package com.syncturtle.services.user.support.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;

public final class TestClocks {

    public static final ZoneId UTC = ZoneOffset.UTC;

    public static final Instant NOW = Instant.parse("2026-05-22T14:00:00Z");

    public static final Instant ONE_MINUTE_BEFORE_NOW = NOW.minusSeconds(60);

    public static final Instant ONE_MINUTE_AFTER_NOW = NOW.plusSeconds(60);

    private TestClocks() {
    }

    public static Clock fixedUtc() {
        return fixedUtc(NOW);
    }

    public static Clock fixedUtc(Instant instant) {
        Objects.requireNonNull(instant, "instant is required");

        return Clock.fixed(instant, UTC);
    }

    public static Clock fixedUtc(String isoInstant) {
        Objects.requireNonNull(isoInstant, "isoInstant is required");

        return fixedUtc(Instant.parse(isoInstant));
    }

    public static MutableClock mutableUtc() {
        return MutableClock.at(NOW, UTC);
    }

    public static MutableClock mutableUtc(Instant instant) {
        return MutableClock.at(instant, UTC);
    }
}