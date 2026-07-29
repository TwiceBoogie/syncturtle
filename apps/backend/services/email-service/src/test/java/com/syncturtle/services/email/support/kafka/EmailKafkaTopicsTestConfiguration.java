package com.syncturtle.services.email.support.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin.NewTopics;
import org.springframework.util.Assert;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.common.kafka.property.KafkaErrorHandlingProperties;

/**
 * Provisions the kafka topology required by email-service integration tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class EmailKafkaTopicsTestConfiguration {

    private static final int TEST_PARTITION_COUNT = 1;
    private static final int TEST_REPLICA_COUNT = 1;

    @Bean
    NewTopics emailEventsTopic(KafkaErrorHandlingProperties errorHandlingProperties) {
        Assert.notNull(errorHandlingProperties, "kafka error handling properties are required");

        return new NewTopics(
                topic(KafkaTopics.EMAIL_EVENTS_V1),
                deadLetterTopic(KafkaTopics.EMAIL_EVENTS_V1, errorHandlingProperties.getDltSuffix()),
                topic(KafkaTopics.INSTANCE_CONFIG_EVENTS_V1),
                deadLetterTopic(KafkaTopics.INSTANCE_CONFIG_EVENTS_V1, errorHandlingProperties.getDltSuffix()));
    }

    private static NewTopic topic(String topicName) {
        Assert.hasText(topicName, "kafka test topic name is required");

        return TopicBuilder.name(topicName.trim())
                .partitions(TEST_PARTITION_COUNT)
                .replicas(TEST_REPLICA_COUNT)
                .build();
    }

    private static NewTopic deadLetterTopic(String sourceTopic, String dltSuffix) {
        Assert.hasText(sourceTopic, "kafka source topic is required");
        Assert.hasText(dltSuffix, "kafka dlt suffix is required");

        return topic(sourceTopic.trim() + dltSuffix.trim());
    }

}
