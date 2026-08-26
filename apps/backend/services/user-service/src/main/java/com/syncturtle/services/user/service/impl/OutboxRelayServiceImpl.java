package com.syncturtle.services.user.service.impl;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.OutboxEvent;
import com.syncturtle.services.user.messaging.kafka.publisher.OutboxKafkaPublisher;
import com.syncturtle.services.user.messaging.outbox.OutboxEnvelope;
import com.syncturtle.services.user.messaging.outbox.OutboxEventDeserializer;
import com.syncturtle.services.user.service.OutboxRelayService;
import com.syncturtle.services.user.service.collaborator.outbox.OutboxMessageClaimer;
import com.syncturtle.services.user.service.collaborator.outbox.OutboxMessageRecorder;
import com.syncturtle.services.user.service.collaborator.outbox.OutboxRetryDelayPolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayServiceImpl implements OutboxRelayService {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofMinutes(5);

    private final OutboxMessageClaimer claimer;
    private final OutboxMessageRecorder recorder;
    private final OutboxEventDeserializer deserializer;
    private final OutboxRetryDelayPolicy retryDelayPolicy;
    private final OutboxKafkaPublisher kafkaPublisher;

    @Override
    public void relayDueMessages(String workerId) {
        Assert.hasText(workerId, "workerId is required");

        releaseExpiredLocks();

        List<OutboxEnvelope> envelopes = claimer.claimDueMessages(workerId, DEFAULT_BATCH_SIZE);

        if (envelopes.isEmpty()) {
            return;
        }

        for (OutboxEnvelope envelope : envelopes) {
            publishOne(envelope);
        }
    }

    private void publishOne(OutboxEnvelope envelope) {
        Assert.notNull(envelope, "outbox envelope is required");

        try {
            OutboxEvent event = deserializer.toEvent(envelope);

            kafkaPublisher.publishOutboxEvent(envelope.getTopic(), envelope.getMessageKey(), event,
                    envelope.getTraceContext());

            recorder.markPublished(envelope.getId());
        } catch (Exception exception) {
            Duration retryDelay = retryDelayPolicy.nextDelay(envelope);

            recorder.markFailed(envelope.getId(), envelope.getMessageKey(), retryDelay);

            log.warn("Outbox publish failed. outboxId={} eventType={} attempts={} retryDelay={} message={}",
                    envelope.getId(), envelope.getEventType(), envelope.getAttempts(), retryDelay,
                    exception.getMessage(), exception);
        }
    }

    private void releaseExpiredLocks() {
        int released = claimer.releaseExpiredPublishingLocks(DEFAULT_LOCK_TIMEOUT, DEFAULT_BATCH_SIZE);

        if (released > 0) {
            log.warn("Released expired outbox publishing locks. released={}", released);
        }
    }

}
