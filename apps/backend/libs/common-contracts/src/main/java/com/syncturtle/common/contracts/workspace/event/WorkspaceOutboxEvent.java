package com.syncturtle.common.contracts.workspace.event;

import java.time.Instant;
import java.util.UUID;

import com.syncturtle.common.contracts.messaging.OutboxEvent;

public interface WorkspaceOutboxEvent extends OutboxEvent {
    @Override
    String getEventId();

    @Override
    Instant getOccurredAt();

    Enum<?> getType();

    UUID getId();

    Instant getDeletedAt();

    Long getVersion();

    boolean isCreateEvent();

    boolean isUpdateEvent();

    boolean isDeleteEvent();

    boolean isTombstoneEvent();

    @Override
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
