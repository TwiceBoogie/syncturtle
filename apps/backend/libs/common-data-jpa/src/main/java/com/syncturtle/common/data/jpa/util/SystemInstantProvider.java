package com.syncturtle.common.data.jpa.util;

import java.time.Instant;

public final class SystemInstantProvider implements InstantProvider {

    @Override
    public Instant now() {
        return Instant.now();
    }

}
