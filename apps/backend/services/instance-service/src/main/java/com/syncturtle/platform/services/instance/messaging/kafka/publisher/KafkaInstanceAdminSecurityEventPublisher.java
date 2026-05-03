package com.syncturtle.platform.services.instance.messaging.kafka.publisher;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.syncturtle.common.core.constants.KafkaTopicConstants;
import com.syncturtle.common.core.events.InstanceAdminSecurityEvent;
import com.syncturtle.platform.services.instance.messaging.InstanceAdminSecurityEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaInstanceAdminSecurityEventPublisher implements InstanceAdminSecurityEventPublisher {

    private final KafkaTemplate<String, Object> kafka;

    @Override
    public void publishInstanceAdminSecurityEvent(InstanceAdminSecurityEvent event) {
        String key = event.getInstanceId() + ":" + event.getUserId();

        kafka.send(KafkaTopicConstants.INSTANCE_ADMIN_SECURITY_EVENTS_V1, key, event)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("Kafka publish failed for admin security event {}: {}", key, ex.getMessage(), ex);
                    } else if (res != null) {
                        RecordMetadata md = res.getRecordMetadata();
                        log.info("Published {} key={} to {}-{}@{}", event.getType(), key, md.topic(), md.partition(),
                                md.offset());
                    }
                });
    }

}
