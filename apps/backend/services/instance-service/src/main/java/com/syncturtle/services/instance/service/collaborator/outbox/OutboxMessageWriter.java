package com.syncturtle.services.instance.service.collaborator.outbox;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.OutboxEvent;
import com.syncturtle.common.observability.tracing.PersistedTraceContext;
import com.syncturtle.common.observability.tracing.TraceContextPropagator;
import com.syncturtle.services.instance.model.OutboxMessage;
import com.syncturtle.services.instance.model.param.OutboxMessageCreateParam;
import com.syncturtle.services.instance.repository.OutboxMessageRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OutboxMessageWriter {

    private static final int DEFAULT_MAX_ATTEMPTS = 20;

    private final OutboxMessageRepository repository;
    private final JsonMapper jsonMapper;
    private final TraceContextPropagator traceContextPropagator;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxMessage save(OutboxEvent event, String topic, String messageKey, String aggregateType,
            UUID aggregateId) {
        Assert.notNull(event, "outbox event is required");
        Assert.hasText(topic, "topic is required");
        Assert.hasText(messageKey, "messageKey is required");
        Assert.hasText(aggregateType, "aggregateType is required");
        Assert.notNull(aggregateId, "aggregateId is required");

        PersistedTraceContext traceContext = traceContextPropagator.capture();

        OutboxMessageCreateParam param = OutboxMessageCreateParam.builder()
                .topic(topic)
                .messageKey(messageKey)
                .eventType(event.eventTypeName())
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(serialize(event))
                .traceparent(traceContext.traceparent())
                .tracestate(traceContext.tracestate())
                .maxAttempts(DEFAULT_MAX_ATTEMPTS)
                .clock(clock)
                .build();

        return repository.save(OutboxMessage.create(param));
    }

    private String serialize(OutboxEvent event) {
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize outbox event. eventType=%s eventId=%s"
                    .formatted(event.eventTypeName(), event.getEventId()), exception);
        }
    }

}
