package com.syncturtle.services.instance.messaging.kafka.publisher;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.instance.event.InstanceEvent;
import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.instance.messaging.InstanceEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaInstanceEventPublisher implements InstanceEventPublisher {

    private static final String KAFKA_EXCEPTION_MESSAGE = "Kafka publish failed. topic={} key={} eventType={} message={}";
    private static final String KAFKA_NULL_RESULT_MESSAGE = "Kafka publish completed without result. topic={} key={} eventType={}";
    private static final String KAFKA_SUCCESS_MESSAGE = "Kafka publish succeeded. eventType={} key={} topic={} partition={} offset={}";

    private final KafkaTemplate<String, Object> kafka;

    @Override
    public void publishInstanceEvent(InstanceEvent event) {
        Assert.notNull(event, "instance event is required");

        String key = event.getId().toString();

        kafka.send(KafkaTopics.INSTANCE_EVENTS_V1, key, event).whenComplete((result, exception) -> {
            if (exception != null) {
                log.error(KAFKA_EXCEPTION_MESSAGE,
                        KafkaTopics.INSTANCE_EVENTS_V1,
                        key,
                        event.getType(),
                        exception.getMessage(),
                        exception);
                return;
            }

            if (result == null) {
                log.warn(KAFKA_NULL_RESULT_MESSAGE, KafkaTopics.INSTANCE_EVENTS_V1, key, event.getType());
                return;
            }

            RecordMetadata metadata = result.getRecordMetadata();
            log.info(
                    KAFKA_SUCCESS_MESSAGE,
                    event.getType(),
                    key,
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset());
        });
    }
}
