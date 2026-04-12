package com.syncturtle.platform.services.email.integration.support;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

import com.syncturtle.common.core.constants.KafkaTopicConstants;

@TestConfiguration(proxyBeanMethods = false)
public class EmailKafkaTopicsTestConfiguration {

    @Bean
    NewTopic emailEventsTopic() {
        return TopicBuilder.name(KafkaTopicConstants.EMAIL_EVENTS_V1)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic emailEventsDltTopic() {
        return TopicBuilder.name(KafkaTopicConstants.EMAIL_EVENTS_V1 + ".DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

}
