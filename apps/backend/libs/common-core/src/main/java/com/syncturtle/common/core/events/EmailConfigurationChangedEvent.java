package com.syncturtle.common.core.events;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfigurationChangedEvent {
    private String eventId;
    private Instant occurredAt;

    private long version;
}
