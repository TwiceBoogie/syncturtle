package com.syncturtle.common.data.jpa.util;

import java.util.UUID;

public final class RandomUuidProvider implements UuidProvider {

    @Override
    public UUID newUuid() {
        return UUID.randomUUID();
    }

}
