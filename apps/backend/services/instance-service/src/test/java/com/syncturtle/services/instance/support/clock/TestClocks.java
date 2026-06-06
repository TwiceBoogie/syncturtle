package com.syncturtle.services.instance.support.clock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class TestClocks {

    public static final Instant NOW = Instant.parse("2026-05-22T14:00:00Z");

    private TestClocks() {
    }

    public static Clock fixedUtc() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    public static Clock fixedUtc(String isoInstant) {
        return Clock.fixed(Instant.parse(isoInstant), ZoneOffset.UTC);
    }

}
