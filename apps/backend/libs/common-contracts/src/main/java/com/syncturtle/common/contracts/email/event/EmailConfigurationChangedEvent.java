package com.syncturtle.common.contracts.email.event;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EmailConfigurationChangedEvent {
    String eventId;
    Instant occurredAt;
    Long version;
}
