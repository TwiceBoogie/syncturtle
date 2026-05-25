package com.syncturtle.services.instance.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "app.kafka.topics")
public class KafkaTopicProperties {
    private final int partitions;
    private final int replicas;

    public KafkaTopicProperties(int partitions, int replicas) {
        if (partitions < 1) {
            throw new IllegalArgumentException("app.kafka.topics.partitions must be >= 1");
        }
        if (replicas < 1) {
            throw new IllegalArgumentException("app.kafka.topics.replicas must be >= 1");
        }
        this.partitions = partitions;
        this.replicas = replicas;
    }
}
