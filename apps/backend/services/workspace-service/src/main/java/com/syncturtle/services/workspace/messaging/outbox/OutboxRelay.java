package com.syncturtle.services.workspace.messaging.outbox;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceOutboxEvent;
import com.syncturtle.services.workspace.messaging.kafka.publisher.OutboxKafkaPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final Duration DEFAULT_LOCK_TIMEOUT = Duration.ofMinutes(5);

    private final OutboxMessageClaimer claimer;
    private final OutboxMessageRecorder recorder;
    private final OutboxEventDeserializer deserializer;
    private final OutboxRetryDelayPolicy retryDelayPolicy;
    private final OutboxKafkaPublisher kafkaPublisher;

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
            WorkspaceOutboxEvent event = deserializer.toWorkspaceEvent(envelope);

            kafkaPublisher.publishWorkspaceOutboxEvent(envelope.getTopic(), envelope.getMessageKey(), event);

            recorder.markPublished(envelope.getId());
        } catch (Exception exception) {
            Duration retryDelay = retryDelayPolicy.nextDelay(envelope);

            recorder.markFailed(envelope.getId(), exception.getMessage(), retryDelay);

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
