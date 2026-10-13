package com.syncturtle.common.kafka.topic;

import org.springframework.util.Assert;

import com.syncturtle.common.kafka.property.KafkaDeploymentProperties;

/**
 * Maps code-owned logical event topics to one installation's physical Kafka
 * namespace.
 */
public final class KafkaDeploymentTopics {

    private final String deploymentId;

    public KafkaDeploymentTopics(KafkaDeploymentProperties properties) {
        this.deploymentId = properties.getDeploymentId();
    }

    public String topic(String logicalTopic) {
        Assert.hasText(logicalTopic, "logical Kafka topic is required");

        String physicalTopic = deploymentId + "." + logicalTopic;

        Assert.isTrue(physicalTopic.length() + KafkaDeadLetterTopics.SUFFIX.length() <= 249,
                "Kafka topic including its dead-letter suffix must fit Kafka's 249 character limit");

        return physicalTopic;
    }

}
