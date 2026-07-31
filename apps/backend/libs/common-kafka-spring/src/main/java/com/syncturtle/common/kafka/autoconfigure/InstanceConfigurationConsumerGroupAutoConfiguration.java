package com.syncturtle.common.kafka.autoconfigure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.syncturtle.common.kafka.group.InstanceConfigurationConsumerGroups;
import com.syncturtle.common.kafka.property.InstanceConfigurationConsumerGroupProperties;

@AutoConfiguration
@ConditionalOnBooleanProperty(prefix = "app.kafka.instance-configuration", name = "enabled")
@EnableConfigurationProperties(InstanceConfigurationConsumerGroupProperties.class)
public class InstanceConfigurationConsumerGroupAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    InstanceConfigurationConsumerGroups instanceConfigurationConsumerGroups(
            @Value("${spring.application.name}") String serviceName,
            InstanceConfigurationConsumerGroupProperties properties) {
        return new InstanceConfigurationConsumerGroups(serviceName, properties);
    }
}
