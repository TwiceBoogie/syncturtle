package com.syncturtle.services.workspace.messaging.outbox;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.messaging.OutboxEvent;
import com.syncturtle.services.workspace.model.OutboxMessage;
import com.syncturtle.services.workspace.model.param.OutboxMessageCreateParam;
import com.syncturtle.services.workspace.repository.OutboxMessageRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxMessageWriter {

    private static final int DEFAULT_MAX_ATTEMPTS = 20;

    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxMessage save(OutboxEvent event, String topic, String messageKey, String aggregateType,
            UUID aggregateId) {
        Assert.notNull(event, "outbox event is required");
        Assert.hasText(topic, "topic is required");
        Assert.hasText(messageKey, "messageKey is required");
        Assert.hasText(aggregateType, "aggregateType is required");
        Assert.notNull(aggregateId, "aggregateId is required");

        OutboxMessageCreateParam param = OutboxMessageCreateParam.builder()
                .topic(topic)
                .messageKey(messageKey)
                .eventType(event.eventTypeName())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(serialize(event))
                .maxAttempts(DEFAULT_MAX_ATTEMPTS)
                .clock(clock)
                .build();

        return repository.save(OutboxMessage.create(param));
    }

    private String serialize(OutboxEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize outbox event. eventType=%s eventId=%s"
                    .formatted(event.eventTypeName(), event.getEventId()), exception);
        }
    }

}
