package com.syncturtle.common.contracts.instance.event;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.contracts.instance.model.InstanceEdition;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InstanceEvent {
    public enum Type {
        INSTANCE_CREATED, INSTANCE_UPDATED, INSTANCE_SOFT_DELETED
    }

    private String eventId;
    private Instant occurredAt;

    private Type type;

    private UUID id;
    private InstanceEdition edition;
    private boolean setupDone;
    private Long version;
    private String apiBaseUrl;
    private boolean test;
    private Instant updatedAt;
    private Instant createdAt;
}
