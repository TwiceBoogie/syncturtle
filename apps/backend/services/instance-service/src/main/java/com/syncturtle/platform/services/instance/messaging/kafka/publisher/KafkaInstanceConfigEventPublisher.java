package com.syncturtle.platform.services.instance.messaging.kafka.publisher;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.syncturtle.common.core.constants.KafkaTopicConstants;
import com.syncturtle.common.core.events.InstanceConfigurationEvent;
import com.syncturtle.platform.services.instance.messaging.InstanceConfigEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaInstanceConfigEventPublisher implements InstanceConfigEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    @Override
    public void publishInstanceConfigurationEvent(InstanceConfigurationEvent event) {
        String key = event.getScope().name();

        kafka.send(KafkaTopicConstants.INSTANCE_CONFIG_EVENTS_V1, key, event)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Kafka publish failed for instance config scope {}: {}", key, ex.getMessage(), ex);
                    } else if (res != null) {
                        RecordMetadata md = res.getRecordMetadata();
                        log.info("Published config-change scope={} key={} to {}-{}@{}", event.getScope(), key,
                                md.topic(), md.partition(), md.offset());
                    }
                });
    }

}
