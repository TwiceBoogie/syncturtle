package com.syncturtle.platform.services.instance.messaging.kafka.publisher;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.syncturtle.common.core.constants.KafkaTopicConstants;
import com.syncturtle.common.core.events.InstanceEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaInstanceEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    public void publishInstanceEvent(InstanceEvent event) {
        String key = event.getId().toString();
        kafka.send(KafkaTopicConstants.INSTANCE_EVENTS_V1, key, event).whenComplete((res, ex) -> {
            if (ex != null) {
                log.error("Kafka publish failed for instance {}: {}", key, ex.getMessage(), ex);
            } else if (res != null) {
                RecordMetadata md = res.getRecordMetadata();
                log.info("Published {} key={} to {}-{}@{}", event.getType(), key, md.topic(), md.partition(),
                        md.offset());
            }
        });
    }
}
