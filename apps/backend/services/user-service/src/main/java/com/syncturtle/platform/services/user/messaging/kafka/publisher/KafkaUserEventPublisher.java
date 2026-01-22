package com.syncturtle.platform.services.user.messaging.kafka.publisher;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.syncturtle.common.core.constants.KafkaTopicConstants;
import com.syncturtle.common.core.events.UserEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public final class KafkaUserEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public void publishUserEvent(UserEvent event) {
        String key = event.getId().toString();
        kafka.send(KafkaTopicConstants.USER_EVENTS_V1, key, event).whenComplete((res, ex) -> {
            if (ex != null) {
                log.error("Kafka publish failed for user {}: {}", key, ex.getMessage(), ex);
            } else if (res != null) {
                RecordMetadata md = res.getRecordMetadata();
                log.info("Published {} key={} to {}-{}@{}", event.getType(), key, md.topic(), md.partition(),
                        md.offset());
            }
        });
    }

}
