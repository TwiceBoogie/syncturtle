package com.syncturtle.common.data.jpa.support;

import java.time.Instant;

public final class SystemInstantProvider implements InstantProvider {

    @Override
    public Instant now() {
        return Instant.now();
    }

}
