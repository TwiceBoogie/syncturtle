package com.syncturtle.services.email.support.clock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class MutableClock extends Clock {

    private final AtomicReference<Instant> currentInstant;
    private final ZoneId zoneId;

    private MutableClock(AtomicReference<Instant> currentInstant, ZoneId zoneId) {
        this.currentInstant = Objects.requireNonNull(currentInstant, "currentInstant is required");
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId is required");
    }

    public static MutableClock at(Instant instant, ZoneId zoneId) {
        Objects.requireNonNull(instant, "instant is required");
        Objects.requireNonNull(zoneId, "zoneId is required");
        return new MutableClock(new AtomicReference<>(instant), zoneId);
    }

    public void advance(Duration duration) {
        Objects.requireNonNull(duration, "duration is required");
        currentInstant.updateAndGet(instant -> instant.plus(duration));
    }

    public void rewind(Duration duration) {
        Objects.requireNonNull(duration, "duration is required");
        currentInstant.updateAndGet(instant -> instant.minus(duration));
    }

    public void setInstant(Instant instant) {
        currentInstant.set(Objects.requireNonNull(instant, "instant is required"));
    }

    @Override
    public ZoneId getZone() {
        return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zoneId) {
        Objects.requireNonNull(zoneId, "zoneId is required");
        return this.zoneId.equals(zoneId) ? this : new MutableClock(currentInstant, zoneId);
    }

    @Override
    public Instant instant() {
        return currentInstant.get();
    }
}
