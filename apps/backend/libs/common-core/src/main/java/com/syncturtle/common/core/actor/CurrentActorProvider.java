package com.syncturtle.common.core.actor;

import java.util.Optional;
import java.util.UUID;

public interface CurrentActorProvider {
    Optional<UUID> currectActorId();
}
