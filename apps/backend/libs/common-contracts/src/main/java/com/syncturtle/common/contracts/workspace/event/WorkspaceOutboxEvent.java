package com.syncturtle.common.contracts.workspace.event;

import java.time.Instant;
import java.util.UUID;

public interface WorkspaceOutboxEvent {
    String getEventId();

    Instant getOccurredAt();

    Enum<?> getType();

    UUID getId();

    Instant getDeletedAt();

    Long getVersion();

    boolean isCreateEvent();

    boolean isUpdateEvent();

    boolean isDeleteEvent();

    boolean isTombstoneEvent();

    default String eventTypeName() {
        return getType().name();
    }

    default UUID aggregateId() {
        return getId();
    }

    default String messageKey() {
        return getId().toString();
    }
}
