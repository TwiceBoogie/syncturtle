package com.syncturtle.common.kafka.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

import com.syncturtle.common.kafka.group.KafkaConsumerGroups;
import com.syncturtle.common.kafka.property.KafkaDeploymentProperties;
import com.syncturtle.common.kafka.topic.KafkaDeploymentTopics;

@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnBean(KafkaTemplate.class)
@EnableConfigurationProperties(KafkaDeploymentProperties.class)
public class KafkaConsumerGroupAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    KafkaDeploymentTopics kafkaDeploymentTopics(KafkaDeploymentProperties properties) {
        return new KafkaDeploymentTopics(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    KafkaConsumerGroups kafkaConsumerGroups(KafkaDeploymentProperties properties) {
        return new KafkaConsumerGroups(properties);
    }

}
