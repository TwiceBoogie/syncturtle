package com.syncturtle.services.instance.configuration.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

import com.syncturtle.common.contracts.messaging.KafkaTopics;
import com.syncturtle.services.instance.configuration.property.KafkaTopicProperties;

import lombok.RequiredArgsConstructor;

@Profile("setup")
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicConfig {

    private final KafkaTopicProperties properties;

    @Bean
    NewTopic instanceEventsTopic() {
        return topic(KafkaTopics.INSTANCE_EVENTS_V1);
    }

    @Bean
    NewTopic instanceConfigEventsTopic() {
        return topic(KafkaTopics.INSTANCE_CONFIG_EVENTS_V1);
    }

    @Bean
    NewTopic userEventsTopic() {
        return topic(KafkaTopics.USER_EVENTS_V1);
    }

    @Bean
    NewTopic workspaceEventsTopic() {
        return topic(KafkaTopics.WORKSPACE_EVENTS_V1);
    }

    @Bean
    NewTopic workspaceMemberEventsTopic() {
        return topic(KafkaTopics.WORKSPACE_MEMBER_EVENTS_V1);
    }

    @Bean
    NewTopic workspaceMemberInviteEventsTopic() {
        return topic(KafkaTopics.WORKSPACE_MEMBER_INVITE_EVENTS_V1);
    }

    @Bean
    NewTopic passwordEventsTopic() {
        return topic(KafkaTopics.PASSWORD_EVENTS_V1);
    }

    @Bean
    NewTopic emailEventsTopic() {
        return topic(KafkaTopics.EMAIL_EVENTS_V1);
    }

    // DLT topics
    @Bean
    NewTopic instanceEventsDltTopic() {
        return dlt(KafkaTopics.INSTANCE_EVENTS_V1);
    }

    @Bean
    NewTopic instanceConfigEventsDltTopic() {
        return dlt(KafkaTopics.INSTANCE_CONFIG_EVENTS_V1);
    }

    @Bean
    NewTopic userEventsDltTopic() {
        return dlt(KafkaTopics.USER_EVENTS_V1);
    }

    @Bean
    NewTopic workspaceEventsDltTopic() {
        return dlt(KafkaTopics.WORKSPACE_EVENTS_V1);
    }

    @Bean
    NewTopic workspaceMemberEventsDltTopic() {
        return dlt(KafkaTopics.WORKSPACE_MEMBER_EVENTS_V1);
    }

    @Bean
    NewTopic workspaceMemberInviteEventsDltTopic() {
        return dlt(KafkaTopics.WORKSPACE_MEMBER_INVITE_EVENTS_V1);
    }

    @Bean
    NewTopic passwordEventsDltTopic() {
        return dlt(KafkaTopics.PASSWORD_EVENTS_V1);
    }

    @Bean
    NewTopic emailEventsDltTopic() {
        return dlt(KafkaTopics.EMAIL_EVENTS_V1);
    }

    private NewTopic topic(String name) {
        return TopicBuilder.name(name)
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .build();
    }

    private NewTopic dlt(String sourceTopic) {
        return TopicBuilder.name(sourceTopic + ".DLT")
                .partitions(properties.getPartitions())
                .replicas(properties.getReplicas())
                .build();
    }

}
