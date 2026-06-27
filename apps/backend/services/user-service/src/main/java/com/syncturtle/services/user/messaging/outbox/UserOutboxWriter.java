package com.syncturtle.services.user.messaging.outbox;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.contracts.user.event.UserEvent;
import com.syncturtle.common.contracts.user.event.UserOutboxEvent;
import com.syncturtle.services.user.model.OutboxMessage;
import com.syncturtle.services.user.model.param.OutboxMessageCreateParam;
import com.syncturtle.services.user.repository.OutboxMessageRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserOutboxWriter {

    private static final String USER_AGGREGATE_TYPE = "User";
    private static final int DEFAULT_MAX_ATTEMPTS = 20;

    private final OutboxMessageRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxMessage saveUserEvent(UserEvent event) {
        return save(event, USER_AGGREGATE_TYPE, KafkaTopics.USER_EVENTS_V1);
    }

    private OutboxMessage save(UserOutboxEvent event, String aggregateType, String kafkaTopic) {
        Assert.notNull(event, "workspace outbox event is required");
        Assert.hasText(aggregateType, "aggregate type is required");
        Assert.hasText(kafkaTopic, "kafkaTopic is required");

        OutboxMessageCreateParam param = OutboxMessageCreateParam.builder()
                .topic(kafkaTopic)
                .messageKey(event.messageKey())
                .eventType(event.eventTypeName())
                .aggregateType(aggregateType)
                .aggregateId(event.aggregateId())
                .payload(serialize(event))
                .maxAttempts(DEFAULT_MAX_ATTEMPTS)
                .clock(clock)
                .build();

        return repository.save(OutboxMessage.create(param));
    }

    private String serialize(UserOutboxEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize user outbox event. eventType=%s, aggregateId=%s"
                    .formatted(event.eventTypeName(), event.aggregateId()), exception);
        }
    }

}
