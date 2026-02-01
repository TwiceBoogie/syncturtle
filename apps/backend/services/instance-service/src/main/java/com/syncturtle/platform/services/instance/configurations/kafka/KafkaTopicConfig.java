package com.syncturtle.platform.services.instance.configurations.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

import com.syncturtle.common.core.constants.KafkaTopicConstants;

@Profile("setup")
@Configuration(proxyBeanMethods = false)
public class KafkaTopicConfig {

    @Bean
    NewTopic instanceEventsTopic() {
        return TopicBuilder.name(KafkaTopicConstants.INSTANCE_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic userEventsTopic() {
        return TopicBuilder.name(KafkaTopicConstants.USER_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic workspaceEventsTopic() {
        return TopicBuilder.name(KafkaTopicConstants.WORKSPACE_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic passwordEventsTopic() {
        return TopicBuilder.name(KafkaTopicConstants.PASSWORD_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    // DLT topics
    @Bean
    NewTopic instanceEventsDltTopic() {
        return TopicBuilder.name(KafkaTopicConstants.INSTANCE_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic userEventsDltTopic() {
        return TopicBuilder.name(KafkaTopicConstants.USER_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic workspaceEventsDltTopic() {
        return TopicBuilder.name(KafkaTopicConstants.WORKSPACE_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic passwordEventsDltTopic() {
        return TopicBuilder.name(KafkaTopicConstants.PASSWORD_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
