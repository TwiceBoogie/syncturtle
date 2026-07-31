package com.syncturtle.common.contracts.messaging;

import java.time.Instant;

public interface OutboxEvent {
    String getEventId();

    Instant getOccurredAt();

    String eventTypeName();
}
