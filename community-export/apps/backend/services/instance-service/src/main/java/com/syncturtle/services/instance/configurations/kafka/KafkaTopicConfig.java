package com.syncturtle.services.instance.configurations.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

import com.syncturtle.common.contracts.messaging.KafkaTopics;

@Profile("setup")
@Configuration(proxyBeanMethods = false)
public class KafkaTopicConfig {

    @Bean
    NewTopic instanceEventsTopic() {
        return TopicBuilder.name(KafkaTopics.INSTANCE_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic instanceConfigEventsTopic() {
        return TopicBuilder.name(KafkaTopics.INSTANCE_CONFIG_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic userEventsTopic() {
        return TopicBuilder.name(KafkaTopics.USER_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic workspaceEventsTopic() {
        return TopicBuilder.name(KafkaTopics.WORKSPACE_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic passwordEventsTopic() {
        return TopicBuilder.name(KafkaTopics.PASSWORD_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic emailEventsTopic() {
        return TopicBuilder.name(KafkaTopics.EMAIL_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    // DLT topics
    @Bean
    NewTopic instanceEventsDltTopic() {
        return TopicBuilder.name(KafkaTopics.INSTANCE_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic instanceConfigEventsDltTopic() {
        return TopicBuilder.name(KafkaTopics.INSTANCE_CONFIG_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic userEventsDltTopic() {
        return TopicBuilder.name(KafkaTopics.USER_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic workspaceEventsDltTopic() {
        return TopicBuilder.name(KafkaTopics.WORKSPACE_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic passwordEventsDltTopic() {
        return TopicBuilder.name(KafkaTopics.PASSWORD_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic emailEventsDltTopic() {
        return TopicBuilder.name(KafkaTopics.EMAIL_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

}
