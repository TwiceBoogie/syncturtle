package com.syncturtle.common.core.actor;

import java.util.UUID;

public final class SystemActors {
    private SystemActors() {
    }

    /**
     * Use when actions are done by the system (setup runner, migrations, etc).
     */
    public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
}
