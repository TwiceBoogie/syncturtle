package com.syncturtle.services.workspace.messaging.kafka.publisher;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.workspace.event.WorkspaceEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxKafkaPublisher {

    private static final Duration DEFAULT_SEND_TIMEOUT = Duration.ofSeconds(10);

    private final KafkaTemplate<String, Object> kafka;

    public void publishWorkspaceEvent(String topic, String messageKey, WorkspaceEvent event) {
        Assert.hasText(topic, "topic is required");
        Assert.hasText(messageKey, "messageKey is required");
        Assert.notNull(event, "workspace event is required");

        try {
            RecordMetadata metadata = kafka.send(topic, messageKey, event)
                    .get(DEFAULT_SEND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
                    .getRecordMetadata();

            log.info("Published outbox workspace event. topic={} key={} eventType={} partition={} offset={}",
                    metadata.topic(), messageKey, event.getType(), metadata.partition(), metadata.offset());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw publishFailed(topic, messageKey, event, exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw publishFailed(topic, messageKey, event, exception);
        }
    }

    private static IllegalStateException publishFailed(String topic, String messageKey, WorkspaceEvent event,
            Exception exception) {
        return new IllegalStateException("Failed to publish workspace outbox event. topic=" + topic + " key="
                + messageKey + " eventType=" + event.getType(), exception);
    }

}
